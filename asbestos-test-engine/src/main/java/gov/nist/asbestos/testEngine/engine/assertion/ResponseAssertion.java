package gov.nist.asbestos.testEngine.engine.assertion;

import gov.nist.asbestos.testEngine.engine.AssertionRunner;
import gov.nist.asbestos.testEngine.engine.FixtureLabels;
import gov.nist.asbestos.testEngine.engine.Reporter;
import gov.nist.asbestos.testEngine.engine.fixture.FixtureComponent;

public class ResponseAssertion {

    public static boolean run(AssertionContext ctx) {
        FixtureComponent sourceFixture = ctx.getSource();
        if (sourceFixture == null) return false;
        FixtureLabels fixtureLabels = ctx.getFixtureLabels();
        if (fixtureLabels == null) return false;

        int codeFound = sourceFixture.getResourceWrapper().getHttpBase().getStatus();
        String found = responseCodeAsString(codeFound);
        String expected = ctx.getCurrentAssert().getResponse().toCode();
        String operator = ctx.getCurrentAssert().hasOperator() ? ctx.getCurrentAssert().getOperator().toCode() : "equals";

        AssertionReport.build(ctx.getCurrentAssertReport(),"Response:",expected,operator,found,sourceFixture,fixtureLabels);
        if(!AssertionRunner.compare(
                ctx,
                found,
                expected,
                operator))
            return false;
        return Reporter.reportPass(ctx, found +" "+operator +" "+expected);
    }

    private static String responseCodeAsString(int code) {
        switch (code) {
            case 200: return "okay";
            case 201: return "created";
            case 204: return "noContent";
            case 304: return "notModified";
            case 400: return "bad";
            case 403: return "forbidden";
            case 404: return "notFound";
            case 405: return "methodNotAllowed";
            case 409: return "conflict";
            case 410: return "gone";
            case 412: return "preconditionFailed";
            case 422: return "unprocessable";
            case 500: return "server failure";
            default: return "CODE_NOT_UNDERSTOOD";
        }
    }

}
