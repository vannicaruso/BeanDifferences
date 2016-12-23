package it.sogei.beanutils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Classe di utilita' per la comparazione tra due oggetti della stessa classe.
 * Espone un solo metodo statico che restituisce una mappa dei campi che hanno valore differente.
 *
 * Confronta anche proprieta' che sono oggetti complessi, liste, mappe e collezioni in genere.
 *
 * La comparazione su collezioni è limitata alla esistenza, non esistenza e comparazione diretta degli oggetti presenti.
 *
 * giovannicaruso,21/12/16.
 */
public class BeanComparator {
    /**
     * Il set di classi wrapper delle primitive, stringhe e altri tipi semplici (date, timestamp, etc.)
     */
    private static final Set<Class> WRAPPERS = new HashSet<>(Arrays.asList(String.class, java.util.Date.class, java.sql.Date.class, java.sql.Timestamp.class,
            Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Void.class));

    /**
     * Costruttore privato per non far istanziare la classe
     */
    private BeanComparator(){}

    /**
     * Restituisce la lista dei campi per cui differiscono i due oggetti passati a parametro
     * @param firstBean l'oggetto sorgente
     * @param secondBean l'oggetto che si vuole comparare
     * @param exclusions array di campi da non verificare (ad es. chiavi dei record che potrebbero
     *                   anche essere differenti)
     * @param <T> la tipologia di classe (serve solo per il type checking)
     * @return la collezione dei campi "diversi"
     * @throws IllegalArgumentException in caso di errore
     * @throws IllegalAccessException in caso di errore
     */
    public static <T> Map<Field, List<Map.Entry<Object, Object>>> compare(T firstBean, T secondBean, String... exclusions) throws IllegalArgumentException, IllegalAccessException {
        // verifiche sui parametri
        if(firstBean == null || secondBean == null)
            throw new IllegalArgumentException("Argomento nullo!");
        if (!firstBean.getClass().equals(secondBean.getClass())){
            throw new IllegalArgumentException("Gli oggetti devono appartenere alla stessa classe!");
        }if (isArray(firstBean))
            throw new IllegalArgumentException("L'argomento e' un array!");
        if (isPrimitive(firstBean) || isPrimitive(secondBean))
            throw new IllegalArgumentException("L'argomento e' una primitiva!");

        // la lista di campi da verificare
        List<Field> fields = getAllFields(firstBean.getClass());
        // la lista dei campi da escludere
        List<String> excludedFields = Arrays.asList(exclusions);

        // la mappa dei campi con valore differente che sara' ritornta dal metodo
        Map<Field, List<Map.Entry<Object, Object>>> differentFields = new HashMap<>();

        Object firstBeanPropertyValue;
        Object secondBeanPropertyValue;

        for(Field field : fields){
            field.setAccessible(true);
            // il campo non deve essere tra quelli esclusi (escudiamo anche i costruttori e i riferimenti a this e superclassi)
            if (excludedFields.contains(field.getName()) || field.getName().startsWith("this$"))
                continue;
            // estraggo i valori
            firstBeanPropertyValue = field.get(firstBean);
            secondBeanPropertyValue = field.get(secondBean);

            //comparo i valori
            if (isPrimitive(firstBeanPropertyValue)){
                differentFields = comparePrimitives(differentFields, field, firstBeanPropertyValue, secondBeanPropertyValue);
            } else if (isCollection(firstBeanPropertyValue)) {
                differentFields = compareCollections(differentFields, field, (Collection) firstBeanPropertyValue, (Collection) secondBeanPropertyValue);
            } else if (isMap(firstBeanPropertyValue)) {
                differentFields = compareMaps(differentFields, field, (Map) firstBeanPropertyValue, (Map) secondBeanPropertyValue);
            } else {
                differentFields = traverse(differentFields, field, firstBeanPropertyValue, secondBeanPropertyValue, excludedFields);
            }

        }
        return differentFields;
    }

    /**
     * Confronta due mappe per chiavi prima e valori dopo
     * @param map la mappa delle differenze
     * @param field il campo che si sta verificando
     * @param firstBeanMap la mappa del primo bean
     * @param secondBeanMap la mappa del secondo bean
     * @return la mappa aggiornata
     * // TODO deep comparison
     */
    private static Map<Field,List<Map.Entry<Object,Object>>> compareMaps(Map<Field, List<Map.Entry<Object, Object>>> map, Field field, Map firstBeanMap, Map secondBeanMap) {
        if (firstBeanMap.keySet().equals(secondBeanMap.keySet())){
            // le chiavi sono uguali, controlliamo i valori...
            for (Object key: firstBeanMap.keySet()){
                if (!firstBeanMap.get(key).equals(secondBeanMap.get(key))){
                    put(map, field, firstBeanMap.get(key), secondBeanMap.get(key));
                }
            }
        } else {
            for (Object key: firstBeanMap.keySet()){
                if (!secondBeanMap.containsKey(key)){
                    put(map, field, firstBeanMap.get(key), null);
                } else {
                    if (!firstBeanMap.get(key).equals(secondBeanMap.get(key))){
                        put(map, field, firstBeanMap.get(key), secondBeanMap.get(key));
                    }
                }
            }
            for (Object key: secondBeanMap.keySet()){
                if (!firstBeanMap.containsKey(key)){
                    put(map, field, null, secondBeanMap.get(key));
                }
            }
        }
        return map;
    }

    /**
     * Confronta due collezioni verificando gli oggetti che sono nella prima e non nella seconda e viceversa.
     * @param map la mappa delle differenze
     * @param field il campo che si sta controllando
     * @param firstBeanCollection la collezione del primo bean
     * @param secondBeanCollection la collezione del secondo bean
     * @return la mappa aggiornata
     * // TODO deep comparison
     */
    private static Map<Field,List<Map.Entry<Object,Object>>> compareCollections(Map<Field, List<Map.Entry<Object, Object>>> map, Field field, Collection firstBeanCollection, Collection secondBeanCollection) {
        for (Object obj: firstBeanCollection){
            if (!secondBeanCollection.contains(obj)){
                put(map, field, obj, null);
            }
        }
        for (Object obj: secondBeanCollection){
            if (!firstBeanCollection.contains(obj)){
                put(map, field, null, obj);
            }
        }
        return map;
    }

    /**
     * Traversa l'oggetto sorgente
     * @param map la mappa dei campi diversi
     * @param field il campo che si sta verificando
     * @param firstBeanPropertyValue il valore della proprieta' del bean a
     * @param secondBeanPropertyValue il valore della proprieta' del bean b
     * @param exclusions i nomi dei campi da escludere dal confronto
     * @return la mappa aggiornata
     * @throws IllegalAccessException in caso di accesso violato
     */
    private static Map<Field, List<Map.Entry<Object, Object>>> traverse (Map<Field, List<Map.Entry<Object, Object>>> map, Field field, Object firstBeanPropertyValue, Object secondBeanPropertyValue, List<String> exclusions) throws IllegalAccessException{
        // array
        if (isArray(firstBeanPropertyValue)) {
            // controllo sulla lunghezza dell'array
            if (((Object[])firstBeanPropertyValue).length > 0) {
                Object[] firstBeanPropertyValueAsArray = (Object[])firstBeanPropertyValue;
                Object[] secondBeanPropertyValueAsArray = (Object[])secondBeanPropertyValue;
                // controllo se e' un array di primitive
                if (isPrimitive(firstBeanPropertyValueAsArray[0]) ) {
                    // verifico la lunghezza degli array
                    if (firstBeanPropertyValueAsArray.length != secondBeanPropertyValueAsArray.length) {
                        // array di lunghezza diversa sono sicuramente diversi
                        put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
                        return map;
                    } else {
                        //comparo le primitive
                        for (int i = 0; i < firstBeanPropertyValueAsArray.length; i++) {
                            map = comparePrimitives(map, field, firstBeanPropertyValueAsArray[i], secondBeanPropertyValueAsArray[i]);
                        }
                    }
                    return map;
                } else if (isArray(firstBeanPropertyValueAsArray[0])){
                    // controllo sulla lunghezza dell'array
                    if (((Object[])firstBeanPropertyValueAsArray[0]).length > 0) {
                        Object[] _arrayFromFirstBean = (Object[])firstBeanPropertyValueAsArray[0];
                        Object[] _arrayFromSecondBean = (Object[])secondBeanPropertyValueAsArray[0];
                        // controllo se e' un array di primitive
                        if (isPrimitive(_arrayFromFirstBean[0]) ) {
                            // verifico la lunghezza degli array
                            if (_arrayFromFirstBean.length != _arrayFromSecondBean.length) {
                                // array di lunghezza diversa sono sicuramente diversi
                                put(map, field, firstBeanPropertyValueAsArray[0], secondBeanPropertyValueAsArray[0]);
                                return map;
                            } else {
                                //comparo le primitive
                                for (int i = 0; i < firstBeanPropertyValueAsArray.length; i++) {
                                    map = comparePrimitives(map, field, firstBeanPropertyValueAsArray[i], secondBeanPropertyValueAsArray[i]);
                                }
                            }
                            return map;
                        } else {
                            return traverseObject(map, field, firstBeanPropertyValue, secondBeanPropertyValue, exclusions);
                        }
                    } else if (secondBeanPropertyValue != null && ((Object[])secondBeanPropertyValue).length > 0){
                        put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
                        return map;
                    } else {
                        return map;
                    }
                } else {
                    return traverseObject(map, field, firstBeanPropertyValue, secondBeanPropertyValue, exclusions);
                }
            } else if (secondBeanPropertyValue != null && ((Object[])secondBeanPropertyValue).length > 0){
                put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
                return map;
            } else {
                return map;
            }
        } else if (isPrimitive(firstBeanPropertyValue)){ // primitiva
            return comparePrimitives(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
        } else { // oggetto generico
            return traverseObject(map, field, firstBeanPropertyValue, secondBeanPropertyValue, exclusions);
        }
    }

    private static Map<Field, List<Map.Entry<Object, Object>>> traverseObject(Map<Field, List<Map.Entry<Object, Object>>> map, Field field, Object firstBeanObject, Object secondBeanObject, List<String> exclusions) throws IllegalAccessException{
        // la lista di campi da verificare
        List<Field> fields = getAllFields(firstBeanObject.getClass());
        Object _firstBeanObject;
        Object _secondBeanObject;
        for(Field _field : fields){
            _field.setAccessible(true);
            // il campo non deve essere tra quelli esclusi
            if (exclusions.contains(_field.getName()) || _field.getName().startsWith("this"))
                continue;
            // estraggo i valori
            _firstBeanObject = _field.get(firstBeanObject);
            _secondBeanObject = _field.get(secondBeanObject);

            //comparo i valori
            if (isPrimitive(_firstBeanObject)){ // primitiva
                map = comparePrimitives(map, _field, _firstBeanObject, _secondBeanObject);
            } else if (isArray(_firstBeanObject)){ // array
                // controllo sulla lunghezza dell'array
                if (((Object[])_firstBeanObject).length > 0) {
                    Object[] _firstBeanObjectAsArray = (Object[])_firstBeanObject;
                    Object[] _secondBeanObjectAsArray = (Object[])_secondBeanObject;
                    // controllo se e' un array di primitive
                    if (isPrimitive(_firstBeanObjectAsArray[0]) ) {
                        // verifico la lunghezza degli array
                        if (_firstBeanObjectAsArray.length != _secondBeanObjectAsArray.length) {
                            // array di lunghezza diversa sono sicuramente diversi
                            put(map, _field, firstBeanObject, secondBeanObject);
                            return map;
                        } else {
                            //comparo le primitive
                            for (int i = 0; i < _firstBeanObjectAsArray.length; i++) {
                                map = comparePrimitives(map, _field, _firstBeanObjectAsArray[i], _secondBeanObjectAsArray[i]);
                            }
                        }
                        return map;
                    } else {
                        return traverse(map, _field, _firstBeanObject, _secondBeanObject, exclusions);
                    }
                } else if (_secondBeanObject != null && ((Object[])_secondBeanObject).length > 0){
                    put(map, _field, _firstBeanObject, _secondBeanObject);
                    return map;
                } else {
                    return map;
                }
            }else { // oggetto
                return traverseObject(map, _field, _firstBeanObject, _secondBeanObject, exclusions);
            }
        }
        return map;
    }

    /**
     * Compara due primitive in base alla tipologia
     * @param map la mappa che contiene i valori diversi delle proprietà dei bean da comparare
     * @param field il campo che si sta confrontando
     * @param firstBeanPropertyValue la proprieta' del primo bean
     * @param secondBeanPropertyValue la proprieta' del secondo bean
     * @return la mappa aggiornata
     */
    private static Map<Field, List<Map.Entry<Object, Object>>> comparePrimitives(Map<Field, List<Map.Entry<Object, Object>>> map, Field field, Object firstBeanPropertyValue, Object secondBeanPropertyValue){
        if (firstBeanPropertyValue instanceof java.lang.String ) {
            if (!((String) firstBeanPropertyValue).equalsIgnoreCase((String)secondBeanPropertyValue))
                put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
        } else if (firstBeanPropertyValue instanceof java.lang.Character) {
            if (firstBeanPropertyValue != secondBeanPropertyValue)
                put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
        } else if (firstBeanPropertyValue instanceof java.lang.Byte){
            if (((Byte) firstBeanPropertyValue).byteValue() != ((Byte) secondBeanPropertyValue).byteValue())
                put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
        } else if (firstBeanPropertyValue instanceof java.lang.Short){
            if (((Short) firstBeanPropertyValue).shortValue() != ((Short) secondBeanPropertyValue).shortValue())
                put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
        } else if (firstBeanPropertyValue instanceof java.lang.Double){
            if (((Double) firstBeanPropertyValue).doubleValue() != ((Double) secondBeanPropertyValue).doubleValue())
                put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
        } else if (firstBeanPropertyValue instanceof java.lang.Float){
            if (((Float) firstBeanPropertyValue).floatValue() != ((Float) secondBeanPropertyValue).floatValue())
                put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
        } else if (firstBeanPropertyValue instanceof java.lang.Integer){
            if (((Integer) firstBeanPropertyValue).intValue() != ((Integer) secondBeanPropertyValue).intValue())
                put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
        } else if (firstBeanPropertyValue instanceof java.sql.Date ){
            if (((java.sql.Date)firstBeanPropertyValue).getTime() != ((java.sql.Date)secondBeanPropertyValue).getTime())
                put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
        } else if (firstBeanPropertyValue instanceof java.sql.Timestamp){
            if (((java.sql.Timestamp)firstBeanPropertyValue).getTime() != ((java.sql.Timestamp)secondBeanPropertyValue).getTime())
                put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
        } else if (firstBeanPropertyValue instanceof java.util.Date ){
            if (((java.util.Date)firstBeanPropertyValue).getTime() != ((java.util.Date)secondBeanPropertyValue).getTime())
                put(map, field, firstBeanPropertyValue, secondBeanPropertyValue);
        }
        return map;
    }

    /**
     * Ricerca tutte le proprietà (private e pubbliche) della gerarchia di classi
     * cui appartiene la classe passata come parametro
     * @param aClass la classe di cui si vuole conoscere la totalità dei campi disponibili
     * @return la lista dei campi che appartengono alla classe passata
     */
    private static List<Field> getAllFields(Class aClass) {
        List<Field> fields = new ArrayList<>();
        do {
            Collections.addAll(fields, aClass.getDeclaredFields());
            aClass = aClass.getSuperclass();
        } while (aClass != null);
        return fields;
    }

    /**
     * Verifica se l'oggetto e' un array
     * @param obj l'oggetto da verificare
     * @return true se array altrimenti false
     */
    private static boolean isArray(Object obj) {
        return obj!=null && (obj instanceof Object[] || obj.getClass().isArray());
    }

    /**
     * Verifica se l'oggetto passato e' una primitiva
     * @param obj l'oggetto
     * @return true se primitiva o wrapper di primitiva altrimenti false
     */
    private static boolean isPrimitive(Object obj){
        return obj != null && isPrimitiveWrapper(obj.getClass());
    }

    /**
     * Verfica se la classe appartiene ad uno dei wrapper delle primitive o e' una stringa (che noi consideriamo una
     * primitiva in questo caso
     * @param clazz la classe da verificare
     * @return true se primitiva altrimenti false
     */
    private static boolean isPrimitiveWrapper(Class clazz) {
        return WRAPPERS.contains(clazz);
    }

    /**
     * Verifica se l'oggetto e' una collezione (liste, set, etc.)
     * @param obj l'oggetto da verificare
     * @return true se l'oggetto e' una collezione
     */
    private static boolean isCollection(Object obj){
        return obj instanceof Collection<?>;
    }

    /**
     * Verifica se l'oggetto e' una implementazione di una mappa (hashmap etc.)
     * @param obj l'oggetto da verificare
     * @return true se l'ogetto e' una mappa
     */
    private static boolean isMap(Object obj){
        return obj instanceof Map<?,?>;
    }
    /**
     * Inserisce un coppia chiave -> valore sulla mappa se il campo non esiste gia',
     * altrimenti aggiunge un elemento nella lista dei valori corrispondenti alla chiave (utile in caso di array)
     * @param map la mappa dei campi differenti
     * @param field il campo che si sta inserendo
     * @param firstBeanPropertyValue l'oggetto del primo bean
     * @param secondBeanPropertyValue l'oggetto del secondo bean
     */
    private static void put(Map<Field, List<Map.Entry<Object, Object>>> map, Field field, Object firstBeanPropertyValue, Object secondBeanPropertyValue) {
        if (map.get(field) == null) {
            List<Map.Entry<Object, Object>> list = new ArrayList<>();
            list.add(new AbstractMap.SimpleEntry(firstBeanPropertyValue, secondBeanPropertyValue));
            map.put(field, list);
        } else {
            map.get(field).add(new AbstractMap.SimpleEntry(firstBeanPropertyValue, secondBeanPropertyValue));
        }
    }
}
