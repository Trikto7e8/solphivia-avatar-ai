package it.solphivia.mobile;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModelEndpointTest {
    private void denied(ModelEndpoint.Kind kind,String url,boolean allow,boolean key){
        try{ModelEndpoint.endpoint(kind,url,allow,key);fail("Unexpected network destination allowed: "+url);}catch(IllegalArgumentException expected){}
    }
    @Test public void officialProviderOriginsCannotBeReplaced(){
        for(ModelEndpoint.Kind k:new ModelEndpoint.Kind[]{ModelEndpoint.Kind.OPENAI,ModelEndpoint.Kind.GEMINI,ModelEndpoint.Kind.CLAUDE})denied(k,"https://other.example",false,true);
        assertEquals("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",ModelEndpoint.endpoint(ModelEndpoint.Kind.GEMINI,"",false,true));
        assertEquals("https://api.anthropic.com/v1/messages",ModelEndpoint.endpoint(ModelEndpoint.Kind.CLAUDE,"",false,true));
    }
    @Test public void cloudAlwaysRequiresHttps(){
        denied(ModelEndpoint.Kind.COMPATIBLE,"http://example.com",true,false);
        denied(ModelEndpoint.Kind.COMPATIBLE,"http://192.168.1.2",true,false);
        assertEquals("https://example.com/v1/chat/completions",ModelEndpoint.endpoint(ModelEndpoint.Kind.COMPATIBLE,"https://example.com/v1/",false,true));
    }
    @Test public void lanHttpRequiresExplicitOptInAndNeverAcceptsKeys(){
        denied(ModelEndpoint.Kind.LM_STUDIO,"http://192.168.1.2:1234",false,false);
        denied(ModelEndpoint.Kind.LM_STUDIO,"http://192.168.1.2:1234",true,true);
        assertEquals("http://192.168.1.2:1234/v1/chat/completions",ModelEndpoint.endpoint(ModelEndpoint.Kind.LM_STUDIO,"http://192.168.1.2:1234",true,false));
    }
    @Test public void localHttpRejectsPublicAddressesAndDnsNames(){
        for(String host:new String[]{"8.8.8.8","172.32.1.2","192.169.1.2","example.com","localhost","127.1","0x7f000001","2130706433","127.000.0.1","[::ffff:127.0.0.1]"})
            denied(ModelEndpoint.Kind.OLLAMA,"http://"+host,true,false);
    }
    @Test public void privateAddressPolicyRejectsAmbiguity(){
        for(String host:new String[]{"10.0.0.1","172.16.0.1","172.31.255.254","192.168.1.5","127.0.0.1"})assertTrue(host,ModelEndpoint.privateIpv4(host));
        for(String host:new String[]{"10.0.0.256","10.0.0.-1","10.00.0.1","10.0.0.1.","10.0.0.1.evil.example"})assertFalse(host,ModelEndpoint.privateIpv4(host));
    }
    @Test public void credentialsQueriesFragmentsAndRelativePathsAreRejected(){
        for(String url:new String[]{"https://user:secret@example.com","https://example.com?key=x","https://example.com/#x","https://example.com/a/../v1","file:///data/file","https://example.com:0","https://example.com:70000"})denied(ModelEndpoint.Kind.COMPATIBLE,url,false,true);
    }
    @Test public void modelCannotInjectHeadersOrControlCharacters(){
        ModelEndpoint.validateModel("qwen3:4b");ModelEndpoint.validateModel("organization/model-v1");
        for(String model:new String[]{"","bad\r\nAuthorization:x","bad model","/bad"})try{ModelEndpoint.validateModel(model);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void requestLimitsCountUtf8Bytes(){
        ModelEndpoint.checkInput("Ciao");
        StringBuilder b=new StringBuilder();for(int i=0;i<12000;i++)b.append('€');
        try{ModelEndpoint.checkInput(b.toString());fail();}catch(IllegalArgumentException expected){}
    }
}
