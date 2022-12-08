package gov.nist.asbestos.services.fixture;


import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FixturePlaceholderReplacer {
    private static Logger log = Logger.getLogger(FixturePlaceholderReplacer.class.getName());
    FixturePartsLoader fixturePartsLoader;

    /**
     * String or substring thereof must not clash with other replacer class begin text tokens
     * example: <!--@{}--> clashes with @{}
     * @return
     */
    public abstract String getBeginText();
    /**
     * String or substring thereof must not clash with other replacer class begin text tokens
     * example: <!--@{}--> clashes with @{}
     * @return
     */

    public abstract String getEndText();
    abstract String getReplacementText(String placeholderName) throws Exception;


    public FixturePlaceholderReplacer(FixturePartsLoader fixturePartsLoader) {
        this.fixturePartsLoader = fixturePartsLoader;
    }

    public String replacePlaceholders(String fixtureString) throws Exception {
        String placeholderBegin = getBeginText();
        String placeholderEnd = getEndText();

        int from =  fixtureString.indexOf(placeholderBegin);
        if (from == -1) {
            // Done, no more placeholders exist to replace
            return fixtureString;
        }
        int to = fixtureString.indexOf(placeholderEnd, from);
        if (to == -1) {
            throw new Exception(String.format("Placeholder at %d has no closing.", from));
        }
        String placeholderName = fixtureString.substring(from+placeholderBegin.length(), to);

        String placeholderValue = fixturePartsLoader.paramsMap.get(placeholderName);
        String placeholderReplacement;
        if (placeholderValue == null) {
            placeholderReplacement = getReplacementText(placeholderName);
            if (placeholderReplacement == null) {
                throw new Exception(String.format("%s fixture placeholder replacement string value cannot be null.", placeholderName));
            }
        } else {
            // TODO: May need other safety checks here
            if (! placeholderValue.contains(getBeginText())) {
                placeholderReplacement = placeholderValue;
            } else {
                String errorMessage = "Safety check failed.";
                log.severe(errorMessage);
                throw new Exception(errorMessage);
            }
        }
        return replacePlaceholders(
                fixtureString.replaceAll(
                        Pattern.quote(String.format("%s%s%s", getBeginText(), placeholderName, getEndText())), Matcher.quoteReplacement(placeholderReplacement)));
    }




}
