package com.goeswhere.dmnp.ue;

import org.eclipse.jdt.core.dom.CatchClause;


/**
 * Where the results go
 */
interface Reporter {
    public void report(final CatchClause cc);
}