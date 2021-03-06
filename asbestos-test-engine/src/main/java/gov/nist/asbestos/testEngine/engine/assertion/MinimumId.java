package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.utilities.ResourceHasMethodsFilter;
import org.hl7.fhir.r4.model.BaseResource;

import java.util.*;

public class MinimumId {
    private boolean isRequest = false;

    public static class Report {
        public List<String> missing = new ArrayList<>();
        public List<String> expected = new ArrayList<>();
        public List<String> errors = new ArrayList<>();


        public Report() {

        }

        Report(String error) {
            errors.add(error);
        }


        void addAll(Report report) {
            missing.addAll(report.missing);
            expected.addAll(report.expected);
        }
    }

    public static Report getReport(String error) {
        return new Report(error);
    }

    public Report run(BaseResource reference, BaseResource sut, boolean isRequest) {
        this.isRequest = isRequest;
        Class<?> miniClass = reference.getClass();
        Class<?> sourceClass = sut.getClass();

        if (!miniClass.equals(sourceClass)) {
            return new Report("minimumId: cannot compare " + miniClass.getName() +  " and " + sourceClass.getName());
        }

        Report report = new Report();
        Set<String> refKeys;
        Set<String> sutKeys;
        List<String> refAtts;
        List<String> diff;
        try {
            Map refMap = ResourceHasMethodsFilter.toMap(reference);
            Map sutMap = ResourceHasMethodsFilter.toMap(sut);

            refKeys = mapKeys("", refMap);
            sutKeys = mapKeys("", sutMap);
            refAtts = new ArrayList<>(refKeys);
            diff = diff(refKeys, sutKeys);

            Collections.sort(refAtts);
        } catch (Throwable t) {
            return new Report(t.getMessage());
        }
        report.expected = refAtts;
        report.expected.remove("description");  // don't know why this shows up but it is wrong
        if (isRequest) {
            report.expected.remove("id");
            //report.expected.remove("created");
            //report.expected.remove("status");
            diff.remove("id");
            //diff.remove("created");
            diff.remove("status");
        }
        List<String> copy = new ArrayList<>(report.expected);
        for (String s : copy) {
            if (s.endsWith(".coding.display"))
                report.expected.remove(s);
        }
        report.missing = diff;
        return report;
    }

    public Set<String> mapKeys(String base, Map ref) {
        Set<String> keys = new HashSet<>();

        for (Object key : ref.keySet()) {
            if (key instanceof String) {
                Object value = ref.get(key);
                if (value instanceof String) {
                    keys.add(base + key);
                } else if (value instanceof Double) {
                    keys.add(base + key);
                } else if (value instanceof Map) {
                    keys.addAll(mapKeys(base + key + ".", (Map) value));
                } else if (value instanceof List) {
                    keys.addAll(mapKeys(base + key + ".", (List) value));
                } else {
                    throw new Error("Do not understand type " + value.getClass().getName());
                }
            } else {
                throw new Error("Do not understand key of type " + key.getClass().getName());
            }
        }

        return keys;
    }

    public Set<String> mapKeys(String base, List ref) {
        // there should only be one element in list but in case there is more...
        Set<String> keys = new HashSet<>();
        for (Object o1 : ref) {
            if (o1 instanceof Map) {
                Set<String> subKeys = mapKeys(base, (Map) o1);
                keys.addAll(subKeys);
            } else if (o1 instanceof String) {
                keys.add((String) o1);
            } else if (o1 instanceof List) {
                keys.addAll(mapKeys(base, (List) o1));
            } else {
                throw new Error("Do not understand type " + o1.getClass().getName());
            }
        }
        return keys;
    }

    public List<String> diff(Set<String> minimum, Set<String> sut) {
        List<String> minList = new ArrayList<>(minimum);
        Collections.sort(minList);
        List<String> diff = new ArrayList<>();
        for (String x : minList) {
            if (!sut.contains(x))
                diff.add(x);
        }
        return diff;
    }

    // what does sut contain beyond the minimum
    public List<String> rdiff(Set<String> minimum, Set<String> sut) {
        List<String> sutList = new ArrayList<>(sut);
        List<String> minList = new ArrayList<>(minimum);
        Collections.sort(sutList);
        List<String> diff = new ArrayList<>();
        for (String x : sutList) {
            if (!minList.contains(x))
                diff.add(x);
        }
        return diff;
    }

    private Report compareKeys(String base, Map ref, Map sut) {
        Report report = new Report();

        Set expected = ref.keySet();
        for (Object o : expected)
            report.expected.add(base + o);
        Set missing1 = difference(sut.keySet(), ref.keySet());
        for (Object m : missing1) {
            if (m instanceof String)
                report.missing.add(base  + m);
            else
                throw new Error(base + ".? is of type " + m.getClass().getName());
        }
        for (Object sutKey : sut.keySet()) {
            Object sutObj = sut.get(sutKey);
            Object refObj = ref.get(sutKey);
            if (sutObj instanceof Map && refObj instanceof Map && sutKey instanceof String) {
                Report report2 = compareKeys(base + sutKey + ".", (Map) refObj, (Map) sutObj);
                report.addAll(report2);
            }
            else if (refObj instanceof List && sutObj instanceof List && sutKey instanceof String) {
                for (Object o : (List) refObj) {
                    report.expected.add(base + o);
                }
                Set missing2 = difference((List) sutObj, (List) refObj);
                int item=0;
                for (Object m : missing2) {
                    if (m instanceof String)
                        report.missing.add(base  + m);
                    else if (m instanceof Map) {
                        Report report2 = new Report();
                    } else
                        throw new Error(base + ".? is of type " + m.getClass().getName());
                    item++;
                }
            } else
                throw new Error("Cannot compare " + base + sutKey);
        }

        return report;
    }

    private Set difference(Set tested, Set base) {
        Set diff = new HashSet<>();

        for (Object item : base) {
            if (!tested.contains(item))
                diff.add(item);
        }
        return diff;
    }

    private Set difference(List tested, List base) {
        Set diff = new HashSet();

        for (Object item : base) {
            if (!tested.contains(item))
                diff.add(item);
        }

        return diff;
    }

}
