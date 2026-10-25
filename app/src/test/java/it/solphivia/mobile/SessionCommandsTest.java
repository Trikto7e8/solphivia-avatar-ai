package it.solphivia.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class SessionCommandsTest {
 @Test public void explicitCloseRequests(){for(String text:new String[]{"Chiudi Solphivia","chiudi l’app!","spegniti","spegni","  chiuditi!  "})assertTrue(text,SessionCommands.close(text));}
 @Test public void questionsNeverClose(){for(String text:new String[]{"Come faccio a chiuderti?","Come ti spengo?","Come si chiude Solphivia?"}){assertTrue(text,SessionCommands.help(text));assertFalse(text,SessionCommands.close(text));}}
 @Test public void ordinarySpeechAndMicrophoneStopAreNotFullClose(){for(String text:new String[]{null,"","non chiuderti","spegni microfono","stop dialogo","ho detto chiudi solphivia ieri","chiudi Maps"})assertFalse(SessionCommands.close(text));}
 @Test public void helpExplainsBothActions(){assertTrue(SessionCommands.CLOSE_HELP.contains("menu"));assertTrue(SessionCommands.CLOSE_HELP.contains("Chiudi Solphivia"));assertTrue(SessionCommands.CLOSE_HELP.contains("Spegni microfono"));}
}
