// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.npm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.net.Authenticator.RequestorType;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import javax.swing.text.html.HTMLEditorKit;

import org.netbeans.spi.keyring.KeyringProvider;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.oauth.IOAuthToken;
import org.openstreetmap.josm.data.oauth.OAuth20Exception;
import org.openstreetmap.josm.data.oauth.OAuth20Parameters;
import org.openstreetmap.josm.data.oauth.OAuth20Token;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.io.DefaultProxySelector;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.auth.AbstractCredentialsAgent;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * The native password manager credentials agent
 */
public class NPMCredentialsAgent extends AbstractCredentialsAgent {

    private KeyringProvider provider;
    private final NPMType type;
    
    /**
     * Cache the results since there might be pop-ups and password prompts from
     * the native manager. This can get annoying, if it shows too often.
     * <br>
     * Yes, there is another cache in AbstractCredentialsAgent. It is used
     * to avoid prompting the user for login multiple times in one session,
     * when they decide not to save the credentials.
     * In contrast, this cache avoids read request the backend in general.
     */
    private final Map<RequestorType, PasswordAuthentication> credentialsCache = new EnumMap<>(RequestorType.class);


    /**
     * Create a new {@link NPMCredentialsAgent}
     * @param type The backend storage type
     */
    public NPMCredentialsAgent(NPMType type) {
        this.type = type;
    }
    
    private KeyringProvider getProvider() {
        if (provider == null) {
            provider = type.getProvider();
        }
        return provider;
    }
    
    protected String getServerDescriptor() {
        String pref = Preferences.main().getPreferenceFile().getAbsolutePath();
        
        String url =  Config.getPref().get("osm-server.url", null);
        if (url == null) {
            url = Config.getUrls().getDefaultOsmApiUrl();
        }
        
        CRC32 id = new CRC32();
        id.update((pref+"/"+url).getBytes(StandardCharsets.UTF_8));
        
        String hash = Integer.toHexString((int)id.getValue());

        return "JOSM.native-password-manager-plugin.api."+hash;
    }
    
    protected String getProxyDescriptor() {
        String pref = Preferences.main().getPreferenceFile().getAbsolutePath();
        String host = Config.getPref().get(DefaultProxySelector.PROXY_HTTP_HOST, "");
        String port = Config.getPref().get(DefaultProxySelector.PROXY_HTTP_PORT, "");
        
        CRC32 id = new CRC32();
        id.update((pref+"/"+host+"/"+port).getBytes(StandardCharsets.UTF_8));

        String hash = Integer.toHexString((int)id.getValue());

        return "JOSM.native-password-manager-plugin.proxy."+hash;
    }
    
    protected String getOAuthDescriptor() {
        String pref = Preferences.main().getPreferenceFile().getAbsolutePath();
        // TODO: put more identifying data here
        
        CRC32 id = new CRC32();
        id.update(pref.getBytes(StandardCharsets.UTF_8));

        String hash = Integer.toHexString((int)id.getValue());

        return "JOSM.native-password-manager-plugin.oauth."+hash;
    }
    
    @Override
    public PasswordAuthentication lookup(RequestorType rt, String host) {
        PasswordAuthentication cache = credentialsCache.get(rt);
        if (cache != null) 
            return cache;
        String user;
        char[] password;
        PasswordAuthentication auth;
        switch(rt) {
            case SERVER:
                if(OsmApi.getOsmApi().getHost().equals(host)) {
                    user = stringNotNull(getProvider().read(getServerDescriptor()+".username"));
                    password = getProvider().read(getServerDescriptor()+".password");
                } else {
                    user = stringNotNull(getProvider().read(host+".username"));
                    password = getProvider().read(host+".password");
                }
                auth = new PasswordAuthentication(user, password == null ? new char[0] : password);
                break;
            case PROXY:
                user = stringNotNull(getProvider().read(getProxyDescriptor()+".username"));
                password = getProvider().read(getProxyDescriptor()+".password");
                auth = new PasswordAuthentication(user, password == null ? new char[0] : password);
                break;
            default: throw new IllegalStateException();
        }
        credentialsCache.put(rt, auth);
        return auth;
    }

    @Override
    public void store(RequestorType rt, String host, PasswordAuthentication credentials) {
        char[] username, password;
        if (credentials == null) {
            username = null;
            password = null;
        } else {
            username = credentials.getUserName() == null ? null : credentials.getUserName().toCharArray();
            password = credentials.getPassword();
        }
        if (username != null && username.length == 0) {
            username = null;
        }
        // password could be empty string in theory, so don't set to null if empty
        String prefix, usernameDescription, passwordDescription;
        switch(rt) {
            case SERVER:
                if(OsmApi.getOsmApi().getHost().equals(host)) {
                    prefix = getServerDescriptor();
                    usernameDescription = tr("JOSM/OSM API/Username");
                    passwordDescription = tr("JOSM/OSM API/Password");
                } else {
                    prefix = host;
                    usernameDescription = tr("{0}/Username", host);
                    passwordDescription = tr("{0}/Password", host);
                }
                break;
            case PROXY:
                prefix = getProxyDescriptor();
                usernameDescription = tr("JOSM/Proxy/Username");
                passwordDescription = tr("JOSM/Proxy/Password");
                break;
            default: throw new IllegalStateException();
        }
        if (username == null) {
            getProvider().delete(prefix+".username");
            getProvider().delete(prefix+".password");
            credentialsCache.remove(rt);
        } else {
            getProvider().save(prefix+".username", username, usernameDescription);
            if (password == null) {
                getProvider().delete(prefix+".password");
            } else {
                getProvider().save(prefix+".password", password, passwordDescription);
            }
            credentialsCache.put(rt, new PasswordAuthentication(stringNotNull(username), password != null ? password : new char[0]));
        }
    }

    @Override
    public IOAuthToken lookupOAuthAccessToken(String host) throws CredentialsAgentException {
        String prolog = getOAuthDescriptor();
        OAuthVersion[] versions = OAuthVersion.values();
        // Prefer newer OAuth protocols
        for (int i = versions.length - 1; i >= 0; i--) {
            OAuthVersion version = versions[i];
            char[] tokenObject = getProvider().read(prolog + ".object." + version + "." + host);
            char[] parametersObject = getProvider().read(prolog + ".parameters." + version + "." + host);
            if (version == OAuthVersion.OAuth20 // There is currently only an OAuth 2.0 path
                    && tokenObject != null && tokenObject.length > 0
                    && parametersObject != null && parametersObject.length > 0) {
                OAuth20Parameters oAuth20Parameters = new OAuth20Parameters(stringNotNull(parametersObject));
                try {
                    return new OAuth20Token(oAuth20Parameters, stringNotNull(tokenObject));
                } catch (OAuth20Exception e) {
                    throw new CredentialsAgentException(e);
                }
            }
        }
        return null;
    }

    @Override
    public void storeOAuthAccessToken(String host, IOAuthToken accessToken) {
        String prolog = getOAuthDescriptor();
        if (accessToken == null) {
            // Assume all oauth tokens must be removed
            for (OAuthVersion version : OAuthVersion.values()) {
                getProvider().delete(prolog + ".object." + version + "." + host);
                getProvider().delete(prolog + ".parameters." + version + "." + host);
            }
        } else {
            OAuthVersion oauthType = accessToken.getOAuthType();
            getProvider().save(prolog + ".object." + oauthType + "." + host,
                    accessToken.toPreferencesString().toCharArray(),
                    tr("JOSM/OAuth/{0}/Token", URI.create(host).getHost()));
            getProvider().save(prolog + ".parameters." + oauthType + "." + host,
                    accessToken.getParameters().toPreferencesString().toCharArray(),
                    tr("JOSM/OAuth/{0}/Parameters", URI.create(host).getHost()));
        }
    }

    private static String stringNotNull(char[] charData) {
        if (charData == null)
            return "";
        return String.valueOf(charData);
    }

    @Override
    public Component getPreferencesDecorationPanel() {
        HtmlPanel pnlMessage = new HtmlPanel();
        HTMLEditorKit kit = (HTMLEditorKit)pnlMessage.getEditorPane().getEditorKit();
        kit.getStyleSheet().addRule(".warning-body {background-color:rgb(253,255,221);padding: 10pt;" +
                "border-color:rgb(128,128,128);border-style: solid;border-width: 1px;}");
        StringBuilder text = new StringBuilder();
        text.append("<html><body><p class=\"warning-body\"><strong>")
                .append(tr("Native Password Manager Plugin"))
                .append("</strong><br>")
                .append(tr("The username and password is protected by {0}.", type.getName()));
        List<String> sensitive = new ArrayList<>();
        if (Config.getPref().get("osm-server.username", null) != null) {
            sensitive.add(tr("username"));
        }
        if (Config.getPref().get("osm-server.password", null) != null) {
            sensitive.add(tr("password"));
        }
        if (Config.getPref().get(DefaultProxySelector.PROXY_USER, null) != null) {
            sensitive.add(tr("proxy username"));
        }
        if (Config.getPref().get(DefaultProxySelector.PROXY_PASS, null) != null) {
            sensitive.add(tr("proxy password"));
        }
        if (Config.getPref().get("oauth.access-token.key", null) != null) {
            sensitive.add(tr("oauth key"));
        }
        if (Config.getPref().get("oauth.access-token.secret", null) != null) {
            sensitive.add(tr("oauth secret"));
        }
        List<String> otherKeys = Config.getPref().getSensitive().stream().sorted().collect(Collectors.toList());
        // Remove keys for which we have specific translated texts
        otherKeys.removeAll(Arrays.asList("osm-server.username", "osm-server.password",
                DefaultProxySelector.PROXY_USER, DefaultProxySelector.PROXY_PASS,
                "oauth.access-token.key", "oauth.access-token.secret"));
        sensitive.addAll(otherKeys);
        if (!sensitive.isEmpty()) {
            text.append(tr("<br><strong>Warning:</strong> There may be sensitive data left in your preference file. ({0})",
                    String.join(", ", sensitive)));
        }
        pnlMessage.setText(text.toString());
        return pnlMessage;
    }
    
    @Override
    public String getSaveUsernameAndPasswordCheckboxText() {
        return tr("Save user and password ({0})", type.getName());
    }
}
