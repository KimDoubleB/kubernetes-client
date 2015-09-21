/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.kubernetes.client;

import io.fabric8.kubernetes.api.model.AuthInfo;
import io.fabric8.kubernetes.api.model.Cluster;
import io.fabric8.kubernetes.api.model.Context;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;
import io.fabric8.kubernetes.client.internal.Utils;
import io.sundr.builder.annotations.Buildable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Config {

  private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

  public static final String KUBERNETES_MASTER_SYSTEM_PROPERTY = "kubernetes.master";
  public static final String KUBERNETES_API_VERSION_SYSTEM_PROPERTY = "kubernetes.api.version";
  public static final String KUBERNETES_TLS_PROTOCOLS_SYSTEM_PROPERTY = "kubernetes.tls.protocols";
  public static final String KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY = "kubernetes.trust.certificates";
  public static final String KUBERNETES_CA_CERTIFICATE_FILE_SYSTEM_PROPERTY = "kubernetes.certs.ca.file";
  public static final String KUBERNETES_CA_CERTIFICATE_DATA_SYSTEM_PROPERTY = "kubernetes.certs.ca.data";
  public static final String KUBERNETES_CLIENT_CERTIFICATE_FILE_SYSTEM_PROPERTY = "kubernetes.certs.client.file";
  public static final String KUBERNETES_CLIENT_CERTIFICATE_DATA_SYSTEM_PROPERTY = "kubernetes.certs.client.data";
  public static final String KUBERNETES_CLIENT_KEY_FILE_SYSTEM_PROPERTY = "kubernetes.certs.client.key.file";
  public static final String KUBERNETES_CLIENT_KEY_DATA_SYSTEM_PROPERTY = "kubernetes.certs.client.key.data";
  public static final String KUBERNETES_CLIENT_KEY_ALGO_SYSTEM_PROPERTY = "kubernetes.certs.client.key.algo";
  public static final String KUBERNETES_CLIENT_KEY_PASSPHRASE_SYSTEM_PROPERTY = "kubernetes.certs.client.key.passphrase";
  public static final String KUBERNETES_AUTH_BASIC_USERNAME_SYSTEM_PROPERTY = "kubernetes.auth.basic.username";
  public static final String KUBERNETES_AUTH_BASIC_PASSWORD_SYSTEM_PROPERTY = "kubernetes.auth.basic.password";
  public static final String KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY = "kubernetes.auth.tryKubeConfig";
  public static final String KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY = "kubernetes.auth.tryServiceAccount";
  public static final String KUBERNETES_OAUTH_TOKEN_SYSTEM_PROPERTY = "kubernetes.auth.token";
  public static final String KUBERNETES_WATCH_RECONNECT_INTERVAL_SYSTEM_PROPERTY = "kubernetes.watch.reconnectInterval";
  public static final String KUBERNETES_WATCH_RECONNECT_LIMIT_SYSTEM_PROPERTY = "kubernetes.watch.reconnectLimit";
  public static final String KUBERNETES_REQUEST_TIMEOUT_SYSTEM_PROPERTY = "kubernetes.request.timeout";
  public static final String KUBERNETES_NAMESPACE_SYSTEM_PROPERTY = "kubernetes.namespace";
  public static final String KUBERNETES_KUBECONFIG_FILE = "kubeconfig";
  public static final String KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";
  public static final String KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
  public static final String KUBERNETES_HTTP_PROXY = "http.proxy";
  public static final String KUBERNETES_HTTPS_PROXY = "https.proxy";
  public static final String KUBERNETES_ALL_PROXY = "all.proxy";
  private Boolean trustCerts;

  private String masterUrl = Utils.getSystemPropertyOrEnvVar(KUBERNETES_MASTER_SYSTEM_PROPERTY, "https://kubernetes.default.svc");
  private String apiVersion = Utils.getSystemPropertyOrEnvVar(KUBERNETES_API_VERSION_SYSTEM_PROPERTY, "v1");
  private String namespace = Utils.getSystemPropertyOrEnvVar(KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "default");
  private String[] enabledProtocols = Utils.getSystemPropertyOrEnvVar(KUBERNETES_TLS_PROTOCOLS_SYSTEM_PROPERTY, "TLSv1.2").split(",");
  private String caCertFile = Utils.getSystemPropertyOrEnvVar(KUBERNETES_CA_CERTIFICATE_FILE_SYSTEM_PROPERTY);
  private String caCertData = Utils.getSystemPropertyOrEnvVar(KUBERNETES_CA_CERTIFICATE_DATA_SYSTEM_PROPERTY);
  private String clientCertFile = Utils.getSystemPropertyOrEnvVar(KUBERNETES_CLIENT_CERTIFICATE_FILE_SYSTEM_PROPERTY);
  private String clientCertData = Utils.getSystemPropertyOrEnvVar(KUBERNETES_CLIENT_CERTIFICATE_DATA_SYSTEM_PROPERTY);
  private String clientKeyFile = Utils.getSystemPropertyOrEnvVar(KUBERNETES_CLIENT_KEY_FILE_SYSTEM_PROPERTY);
  private String clientKeyData = Utils.getSystemPropertyOrEnvVar(KUBERNETES_CLIENT_KEY_DATA_SYSTEM_PROPERTY);
  private String clientKeyAlgo = Utils.getSystemPropertyOrEnvVar(KUBERNETES_CLIENT_KEY_ALGO_SYSTEM_PROPERTY, "RSA");
  private String clientKeyPassphrase = Utils.getSystemPropertyOrEnvVar(KUBERNETES_CLIENT_KEY_PASSPHRASE_SYSTEM_PROPERTY, "changeit");
  private String username = Utils.getSystemPropertyOrEnvVar(KUBERNETES_AUTH_BASIC_USERNAME_SYSTEM_PROPERTY);
  private String password = Utils.getSystemPropertyOrEnvVar(KUBERNETES_AUTH_BASIC_PASSWORD_SYSTEM_PROPERTY);
  private String oauthToken = Utils.getSystemPropertyOrEnvVar(KUBERNETES_OAUTH_TOKEN_SYSTEM_PROPERTY);
  private int watchReconnectInterval = Integer.parseInt(Utils.getSystemPropertyOrEnvVar(KUBERNETES_WATCH_RECONNECT_INTERVAL_SYSTEM_PROPERTY, "1000"));
  private int watchReconnectLimit = Integer.parseInt(Utils.getSystemPropertyOrEnvVar(KUBERNETES_WATCH_RECONNECT_LIMIT_SYSTEM_PROPERTY, "-1"));
  private int requestTimeout = Integer.parseInt(Utils.getSystemPropertyOrEnvVar(KUBERNETES_REQUEST_TIMEOUT_SYSTEM_PROPERTY, "10000"));
  private String proxy = Utils.getSystemPropertyOrEnvVar(KUBERNETES_HTTP_PROXY, Utils.getSystemPropertyOrEnvVar(KUBERNETES_HTTPS_PROXY, Utils.getSystemPropertyOrEnvVar(KUBERNETES_ALL_PROXY)));

  private Map<Integer, String> errorMessages = new HashMap<>();

  public Config() {
    if (Utils.getSystemPropertyOrEnvVar(KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, true)) {
      tryServiceAccount();
    }

    if (Utils.getSystemPropertyOrEnvVar(KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, true)) {
      tryKubeConfig();
    }


    if (!this.masterUrl.endsWith("/")) {
      this.masterUrl = this.masterUrl + "/";
    }

    this.masterUrl = this.masterUrl + "api/" + this.apiVersion + "/";
    this.trustCerts = this.trustCerts != null ? this.trustCerts : Boolean.FALSE;
  }

  @Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
  public Config(String masterUrl, String apiVersion, String namespace, String[] enabledProtocols, Boolean trustCerts, String caCertFile, String caCertData, String clientCertFile, String clientCertData, String clientKeyFile, String clientKeyData, String clientKeyAlgo, String clientKeyPassphrase, String username, String password, String oauthToken, int watchReconnectInterval, int watchReconnectLimit, int requestTimeout, String proxy) {
    this.masterUrl = masterUrl;
    this.apiVersion = apiVersion;
    this.namespace = namespace;
    this.enabledProtocols = enabledProtocols;
    this.trustCerts = trustCerts;
    this.caCertFile = caCertFile;
    this.caCertData = caCertData;
    this.clientCertFile = clientCertFile;
    this.clientCertData = clientCertData;
    this.clientKeyFile = clientKeyFile;
    this.clientKeyData = clientKeyData;
    this.clientKeyAlgo = clientKeyAlgo;
    this.clientKeyPassphrase = clientKeyPassphrase;
    this.username = username;
    this.password = password;
    this.oauthToken = oauthToken;
    this.watchReconnectInterval = watchReconnectInterval;
    this.watchReconnectLimit = watchReconnectLimit;
    this.requestTimeout = requestTimeout;
    this.proxy = proxy;

    if (Utils.getSystemPropertyOrEnvVar(KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, true)) {
      tryServiceAccount();
    }
    if (Utils.getSystemPropertyOrEnvVar(KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, true)) {
      tryKubeConfig();
    }

    if (!this.masterUrl.endsWith("/")) {
      this.masterUrl = this.masterUrl + "/";
    }

    this.masterUrl = this.masterUrl + "api/" + this.apiVersion + "/";
    this.trustCerts = this.trustCerts != null ? this.trustCerts : Boolean.FALSE;
  }

  private void tryServiceAccount() {
    boolean serviceAccountCaCertExists = Files.isRegularFile(Paths.get(KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH));
    if (serviceAccountCaCertExists) {
      caCertFile = clientCertFile != null ? caCertFile : KUBERNETES_KUBECONFIG_FILE;
    }
    try {
      String serviceTokenCandidate = new String(Files.readAllBytes(Paths.get(KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH)));


      if ((oauthToken == null || oauthToken.isEmpty()) && serviceTokenCandidate != null) {
        oauthToken = serviceTokenCandidate;

        String txt = "Configured service account doesn't have access. Service account may have been revoked.";
        errorMessages.put(401, "Unauthorized! " + txt);
        errorMessages.put(403, "Forbidden!" + txt);
      }
    } catch (IOException e) {
      // No service account token available...
    }
  }

  private void tryKubeConfig() {
    String kubeConfigFile = Utils.getSystemPropertyOrEnvVar(KUBERNETES_KUBECONFIG_FILE, new File(System.getProperty("user.home", "."), ".kube" + File.separator + "config").toString());
    boolean kubeConfigFileExists = Files.isRegularFile(Paths.get(kubeConfigFile));
    if (kubeConfigFileExists) {
      try {
        io.fabric8.kubernetes.api.model.Config kubeConfig = KubeConfigUtils.parseConfig(new File(kubeConfigFile));
        Context currentContext = KubeConfigUtils.getCurrentContext(kubeConfig);
        Cluster currentCluster = KubeConfigUtils.getCluster(kubeConfig, currentContext);
        if (currentCluster != null) {
          masterUrl = masterUrl != null ? masterUrl : currentCluster.getServer();
          namespace = namespace != null ? namespace : currentContext.getNamespace();
          caCertFile = caCertFile != null ? caCertFile : currentCluster.getCertificateAuthority();
          caCertData = caCertData != null ? caCertData : currentCluster.getCertificateAuthorityData();
          trustCerts = trustCerts != null ? trustCerts : (currentCluster.getInsecureSkipTlsVerify() != null && currentCluster.getInsecureSkipTlsVerify());

          AuthInfo currentAuthInfo = KubeConfigUtils.getUserAuthInfo(kubeConfig, currentContext);
          if (currentAuthInfo != null) {
            clientCertFile = clientKeyFile != null ? clientCertFile : currentAuthInfo.getClientCertificate();
            clientCertData = clientCertData != null ? clientCertData : currentAuthInfo.getClientCertificateData();
            clientKeyFile = clientKeyFile != null ? clientKeyFile : currentAuthInfo.getClientKey();
            clientKeyData = clientKeyData != null ? clientKeyData : currentAuthInfo.getClientKeyData();
            oauthToken = oauthToken != null ? oauthToken : currentAuthInfo.getToken();
            username = username != null ? username : currentAuthInfo.getUsername();
            password = password != null ? password : currentAuthInfo.getPassword();

            String txt = "Token may have expired! Please use kubectl login / oc login to log-in again.";
            errorMessages.put(401, "Unauthorized! " + txt);
            errorMessages.put(403, "Forbidden!" + txt);
          }
        }
      } catch (IOException e) {
        LOGGER.error("Could not load kube config file from {}", kubeConfigFile, e);
      }
    }
  }

  public String getOauthToken() {
    return oauthToken;
  }

  public void setOauthToken(String oauthToken) {
    this.oauthToken = oauthToken;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getClientKeyPassphrase() {
    return clientKeyPassphrase;
  }

  public void setClientKeyPassphrase(String clientKeyPassphrase) {
    this.clientKeyPassphrase = clientKeyPassphrase;
  }

  public String getClientKeyAlgo() {
    return clientKeyAlgo;
  }

  public void setClientKeyAlgo(String clientKeyAlgo) {
    this.clientKeyAlgo = clientKeyAlgo;
  }

  public String getClientKeyData() {
    return clientKeyData;
  }

  public void setClientKeyData(String clientKeyData) {
    this.clientKeyData = clientKeyData;
  }

  public String getClientKeyFile() {
    return clientKeyFile;
  }

  public void setClientKeyFile(String clientKeyFile) {
    this.clientKeyFile = clientKeyFile;
  }

  public String getClientCertData() {
    return clientCertData;
  }

  public void setClientCertData(String clientCertData) {
    this.clientCertData = clientCertData;
  }

  public String getClientCertFile() {
    return clientCertFile;
  }

  public void setClientCertFile(String clientCertFile) {
    this.clientCertFile = clientCertFile;
  }

  public String getCaCertData() {
    return caCertData;
  }

  public void setCaCertData(String caCertData) {
    this.caCertData = caCertData;
  }

  public String getCaCertFile() {
    return caCertFile;
  }

  public void setCaCertFile(String caCertFile) {
    this.caCertFile = caCertFile;
  }

  public String[] getEnabledProtocols() {
    return enabledProtocols;
  }

  public void setEnabledProtocols(String[] enabledProtocols) {
    this.enabledProtocols = enabledProtocols;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public String getMasterUrl() {
    return masterUrl;
  }

  public void setMasterUrl(String masterUrl) {
    this.masterUrl = masterUrl;
  }

  public boolean isTrustCerts() {
    return trustCerts != null ? trustCerts : false;
  }

  public void setTrustCerts(boolean trustCerts) {
    this.trustCerts = trustCerts;
  }

  public int getWatchReconnectInterval() {
    return watchReconnectInterval;
  }

  public void setWatchReconnectInterval(int watchReconnectInterval) {
    this.watchReconnectInterval = watchReconnectInterval;
  }

  public int getWatchReconnectLimit() {
    return watchReconnectLimit;
  }

  public void setWatchReconnectLimit(int watchReconnectLimit) {
    this.watchReconnectLimit = watchReconnectLimit;
  }

  public Map<Integer, String> getErrorMessages() {
    return errorMessages;
  }

  public void setErrorMessages(Map<Integer, String> errorMessages) {
    this.errorMessages = errorMessages;
  }

  public static ConfigBuilder builder() {
    return new ConfigBuilder();
  }

  public int getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(int requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public void setProxy(String proxy) {
    this.proxy = proxy;
  }

  public String getProxy() {
    return proxy;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }
}
