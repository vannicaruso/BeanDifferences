package it.sogei.beanutils;

import org.junit.Test;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by giovannicaruso on 22/12/16.
 */
public class BeanComparatorTest {
    @Test(expected=IllegalArgumentException.class)
    public void compareDifferentClasses() throws Exception {
        SimpleBean a = new SimpleBean(1, "1");
        ComplexBean b = new ComplexBean(1, "2", null);
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(a, b, "id");
    }

    @org.junit.Test
    public void compareSimpleObjects() throws Exception {
        SimpleBean a = new SimpleBean(1, "a");
        SimpleBean b = new SimpleBean(2, "b");
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(a, b, "id");
        assertNotNull(map);
    }

    @org.junit.Test
    public void compareSimpleObjectsWithoutExclusions() throws Exception {
        SimpleBean a = new SimpleBean(1, "a");
        SimpleBean b = new SimpleBean(2, "b");
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(a, b);
        assertTrue(map.keySet().size() == 2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void comparePrimitiveArrays() throws Exception {
        int[] array1 = new int[]{1,2,3,4};
        int[] array2 = new int[]{1,2,3,4};
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(array1, array2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void compareObjectArrays() throws Exception {
        Integer[] array1 = new Integer[]{1,2,3,4};
        Integer[] array2 = new Integer[]{1,2,3,4};
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(array1, array2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void compareNulls() throws Exception {
        int[] array1 = null;
        int[] array2 = new int[]{1,2,3,4};
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(array1, array2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void comparePrimitives() throws Exception {
        int i = 0;
        int[] array2 = new int[]{1,2,3,4};
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(i, array2);
    }

    @org.junit.Test
    public void compareComplexObjectsWithDifferentArray() throws Exception {
        ComplexBean a = new ComplexBean(1, "a", new String[]{"c", "d"});
        ComplexBean b = new ComplexBean(1, "b", new String[]{"e", "d"});
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(a, b, "id");
        Set<Field> fields = map.keySet();
        assertTrue(fields.size() == 2);
    }

    @org.junit.Test
    public void compareComplexObjectsWithSameArray() throws Exception {
        ComplexBean a = new ComplexBean(1, "a", new String[]{"c", "d"});
        ComplexBean b = new ComplexBean(1, "b", new String[]{"c", "d"});
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(a, b, "id");
        Set<Field> fields = map.keySet();
        assertTrue(fields.size() == 1);
    }

    @org.junit.Test
    public void compareSuperComplexObjectsWithDifferentArray() throws Exception {
        SuperComplexBean a = new SuperComplexBean(1, "a", new String[]{"c", "d"}, new SimpleBean(hashCode(), "ff"));
        SuperComplexBean b = new SuperComplexBean(2, "b", new String[]{"e", "d"}, new SimpleBean(hashCode(), "ff"));
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(a, b, "id");
        Set<Field> fields = map.keySet();
        assertTrue(fields.size() == 2);
    }

    @org.junit.Test
    public void compareSuperComplexObjectsWithDifferentArrays() throws Exception {
        SuperComplexBean a = new SuperComplexBean(1, "a", new String[]{"c", "d", "f"}, new SimpleBean(hashCode(), "ff"));
        SuperComplexBean b = new SuperComplexBean(2, "b", new String[]{"e", "d", "g"}, new SimpleBean(hashCode(), "ff"));
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(a, b, "id");
        Set<Field> fields = map.keySet();
        assertTrue(fields.size() == 2);
    }

    @org.junit.Test
    public void compareSimpleClassWithDateProperties() throws Exception {
        java.util.Date date = new java.util.Date();
        java.sql.Date sqlDate = new java.sql.Date(date.getTime());
        java.sql.Timestamp timestamp = new java.sql.Timestamp(date.getTime());
        java.sql.Timestamp timestamp2 = new java.sql.Timestamp(date.getTime() + 1000000);

        SimpleClassWithDateProperties a = new SimpleClassWithDateProperties(date, sqlDate, timestamp);
        SimpleClassWithDateProperties b = new SimpleClassWithDateProperties(date, sqlDate, timestamp2);
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(a, b);
        Set<Field> fields = map.keySet();
        assertTrue(fields.size() == 1);
    }

    @org.junit.Test
    public void compareSimpleClassWithCollections() throws Exception {
        SimpleClassWithCollections a = new SimpleClassWithCollections(Arrays.asList("a", "b"));
        SimpleClassWithCollections b = new SimpleClassWithCollections(Arrays.asList("a", "c"));
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(a, b);
        Set<Field> fields = map.keySet();
        assertTrue(fields.size() == 1);
    }

    @Test
    public void compareSimpleClassWithMap() throws Exception {
        SimpleClassWithMap a = new SimpleClassWithMap(new HashMap<String, String>()
        {{
            put("One", "1");
            put("Two", "2");
            put("Three", "3");
        }});
        SimpleClassWithMap b = new SimpleClassWithMap(new HashMap<String, String>()
        {{
            put("One", "1");
            put("Two", "2");
            put("Four", "4");
        }});
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(a, b);
        Set<Field> fields = map.keySet();
        assertTrue(fields.size() == 1);
    }

    @Test
    public void compareSuperComplexMegaBean() throws Exception{
        SuperComplexMegaBean a = new SuperComplexMegaBean(1, "a", 2.0d, Arrays.asList(1.0f, 2.0f), Arrays.asList(new SimpleBean(1, "1")), new ComplexBean(2, "2", new String[]{"q", "w"}));
        SuperComplexMegaBean b = new SuperComplexMegaBean(2, "b", 2.1d, Arrays.asList(1.0f, 2.0f), Arrays.asList(new SimpleBean(2, "4")), new ComplexBean(2, "2", new String[]{"q", "w"}));
        Map<Field, List<Map.Entry<Object, Object>>> map = BeanComparator.compare(a, b, "id");
        Set<Field> fields = map.keySet();
        assertTrue(fields.size() == 3);
    }



    class SimpleBean {
        private int id;
        private String desc;

        SimpleBean(int id, String desc) {
            this.id = id;
            this.desc = desc;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }

    class ComplexBean extends SimpleBean{
        private String[] stringArray;

        public ComplexBean(int id, String desc, String[] stringArray) {
            super(id, desc);
            this.stringArray = stringArray;
        }

        public String[] getStringArray() {
            return stringArray;
        }

        public void setStringArray(String[] stringArray) {
            this.stringArray = stringArray;
        }
    }

    class SuperComplexBean extends ComplexBean{
        private SimpleBean s;

        public SuperComplexBean(int id, String desc, String[] stringArray, SimpleBean s) {
            super(id, desc, stringArray);
            this.s = s;
        }

        public SimpleBean getS() {
            return s;
        }

        public void setS(SimpleBean s) {
            this.s = s;
        }
    }

    class SimpleClassWithDateProperties{
        private java.util.Date date;
        private java.sql.Date sqlDate;
        private java.sql.Timestamp timestamp;

        public SimpleClassWithDateProperties(Date date, java.sql.Date sqlDate, Timestamp timestamp) {
            this.date = date;
            this.sqlDate = sqlDate;
            this.timestamp = timestamp;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public java.sql.Date getSqlDate() {
            return sqlDate;
        }

        public void setSqlDate(java.sql.Date sqlDate) {
            this.sqlDate = sqlDate;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Timestamp timestamp) {
            this.timestamp = timestamp;
        }
    }

    class SimpleClassWithCollections{
        List<String> lista;

        public SimpleClassWithCollections(List<String> lista) {
            this.lista = lista;
        }

        public List<String> getLista() {
            return lista;
        }

        public void setLista(List<String> lista) {
            this.lista = lista;
        }
    }

    class SimpleClassWithMap{
        Map<String, String> map;

        public SimpleClassWithMap(Map<String, String> map) {
            this.map = map;
        }

        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(Map<String, String> map) {
            this.map = map;
        }
    }

    class SuperComplexMegaBean{
        int id;
        String s;
        Double d;
        List<Float> floatList;
        List<SimpleBean> simpleBeanList;
        ComplexBean complexBean;

        public SuperComplexMegaBean(int id, String s, Double d, List<Float> floatList, List<SimpleBean> simpleBeanList, ComplexBean complexBean) {
            this.id = id;
            this.s = s;
            this.d = d;
            this.floatList = floatList;
            this.simpleBeanList = simpleBeanList;
            this.complexBean = complexBean;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }

        public Double getD() {
            return d;
        }

        public void setD(Double d) {
            this.d = d;
        }

        public List<Float> getFloatList() {
            return floatList;
        }

        public void setFloatList(List<Float> floatList) {
            this.floatList = floatList;
        }

        public List<SimpleBean> getSimpleBeanList() {
            return simpleBeanList;
        }

        public void setSimpleBeanList(List<SimpleBean> simpleBeanList) {
            this.simpleBeanList = simpleBeanList;
        }

        public ComplexBean getComplexBean() {
            return complexBean;
        }

        public void setComplexBean(ComplexBean complexBean) {
            this.complexBean = complexBean;
        }
    }

}