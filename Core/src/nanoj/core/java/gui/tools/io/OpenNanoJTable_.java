package nanoj.core.java.gui.tools.io;

import ij.measure.ResultsTable;
import nanoj.core.java.gui._BaseDialog_;
import nanoj.core.java.io.LoadNanoJTable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 31/01/16
 * Time: 14:53
 */
public class OpenNanoJTable_  extends _BaseDialog_ {

    @Override
    public boolean beforeSetupDialog(String arg) {
        useSettingsObserver = false;
        autoOpenImp = false;
        return true;
    }

    @Override
    public void setupDialog() {

    }

    @Override
    public boolean loadSettings() {
        return true;
    }

    @Override
    public void execute() throws InterruptedException, IOException {
        LoadNanoJTable LNT = new LoadNanoJTable();
        ResultsTable rt = LNT.getResultsTable();
        //Analyzer.setResultsTable(rt);
        rt.show(new File(LNT.path).getName());

        Map<String, String> metadata = LNT.getMetaData();
        String comments = LNT.getComments();

        log.msg("NJT: "+LNT.path);
        log.msg("~~NJT Meta-Data~~");
        for(String variableName: metadata.keySet()) log.msg(variableName+": "+metadata.get(variableName));
        log.msg("~~NJT Comments~~");
        if (LNT.getComments().length() == 0) log.msg("None");
        else log.msg(comments);
    }
}
