//	1.在httpclient发起请求时，有时会出现下面这种情况
//	你的日志中出现有关SSL的异常，javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated，我们会注意到SSL这几个字母，这是和https提交时有关的地方。
//
//	2.什么是SSL证书
//	SSL证书，也称为服务器SSL证书，是遵守SSL协议的一种数字证书，由全球信任的证书颁发机构(CA)验证服务器身份后颁发。将SSL证书安装在网站服务器上，可实现网站身份验证和数据加密传输双重功能，有效防止机密数据在传输过程中被窃取和纂改，有效防止钓鱼网站浑水摸鱼盗取用户财产。
//			
//	3.解决方法--忽略SSL证书
//	
//	3.1忽略SSL证书的流程
//	简介：需要告诉client使用一个不同的TrustManager。TrustManager是一个检查给定的证书是否有效的类。SSL使用的模式是X.509,对于该模式Java有一个特定的TrustManager,称为X509TrustManager。首先我们需要创建这样的TrustManager。将TrustManager设置到我们的HttpClient。TrustManager只是被SSL的Socket所使用。Socket通过SocketFactory创建。对于SSL Socket,有一个SSLSocketFactory。当创建新的SSLSocketFactory时,你需要传入SSLContext到它的构造方法中。在SSLContext中,我们将包含我们新创建的TrustManager。
//	创建的TrustManager
//	创建SSLContext:TLS是SSL的继承者,但是它们使用相同的SSLContext。
//	创建SSLSocketFactory
//	将SSLSocketFactory注册到我们的HttpClient上。这是在SchemeRegistry中完成的。
//	创建ClientConnectionManager，创建SchemeRegistry。
//	生成HttpClient

//	对应的httpclient提交为：
//
//		HttpClient httpClient = WebClientDevWrapper.wrapClient(new DefaultHttpClient());
//		HttpPost post = new HttpPost(url);
//	
//	HttpClient httpClient = WebClientDevWrapper.wrapClient(new DefaultHttpClient());
//	HttpPost post = new HttpPost(url);
//	注：这里有一点要说明，有的时候即使加入这段代码也还是会抛异常，个人在查找资料的时候发现，javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated，这种异常还与jdk版本有关，在上边代码的第19行（TLS），这里可以换上与你jdk环境相对应的通信协议TLSv：
//	jdk6默认对应的是TLSv1；但还可以是TLSv1.1和SSLv3。
//	jdk7默认对应的是TLSv1；但还可以是TLSv1.1、TLSv1.2和SSLv3。
//	jdk8默认对应的是TLSv1.2；但还可以是TLSv、TLSv1.1和SSLv3。
//	所以要根据自身的实际情况更改对应的通信协议。

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;

public class WebClientDevWrapper {

    public static HttpClient wrapClient(HttpClient base) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
            };
            ctx.init(null, new TrustManager[] { tm }, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            ClientConnectionManager ccm=base.getConnectionManager();
            SchemeRegistry registry = ccm.getSchemeRegistry();
            registry.register(new Scheme("https", 443, ssf));
            return new DefaultHttpClient(ccm, base.getParams());

           /* SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("https", 443, ssf));
            ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(registry);
            return new DefaultHttpClient(mgr, base.getParams());*/
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
