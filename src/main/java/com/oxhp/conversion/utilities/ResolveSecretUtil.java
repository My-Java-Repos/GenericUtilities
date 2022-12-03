package com.oxhp.conversion.utilities;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResolveSecretUtil {

	public static final Logger logger = LogManager.getLogger(ResolveSecretUtil.class);

	private static final Properties secretProperties = new Properties();

	/**
	 * Method to get properties for Pwd related
	 * 
	 * @return
	 */
	public static Properties getSecretProperties() {
		if (secretProperties.isEmpty()) {
			String filePath = Constants.DEFAULT_SECRET_PROPERTIES_FILE;
			if (System.getProperty(Constants.SECRET_PROP_FILE_SYSTEM_ARGS) != null)
				filePath = System.getProperty(Constants.SECRET_PROP_FILE_SYSTEM_ARGS);

			loadProps(secretProperties, filePath);
		}
		return secretProperties;
	}

	/**
	 * Method to load properties file as specified by filePath in to prop Object
	 * 
	 * @param prop
	 * @param filePath
	 */
	private static void loadProps(Properties prop, String filePath) {
		try {
			prop.load(new BufferedInputStream(new FileInputStream(filePath)));
		} catch (IOException e) {
			logger.error("IO Exception Occurred in loading Properties file in ResolveSecret class. Error Message:{}",
					e.getMessage());
		}
	}

	/**
	 * Method to fetch Decrypted secret from Shell script
	 * 
	 * @param secret
	 * @param enclosingStr
	 * @return Decrypted Secret
	 */
	public static String getSecret(String secret, String enclosingStr) {
		logger.debug("START: getSecret method to fetch decrypted secret from Shell script.");
		if (secret == null || enclosingStr == null || enclosingStr.length() == 0
				|| secret.length() <= 2 * enclosingStr.length())
			return secret;

		String retSecret = secret;
		HostExecuteGetStdOutput_outParm ret = null;
		String startStr = secret.substring(0, enclosingStr.length());
		String endStr = secret.substring(secret.length() - enclosingStr.length(), secret.length());

		if (enclosingStr.equals(startStr) && enclosingStr.equals(endStr)) {
			logger.debug("Secret Key found. Executing shell script for password/secret. Key:{}", secret);
			Properties props = getSecretProperties();
			if (!props.isEmpty()) {

				String textPath = Constants.LEGACY_TXT_PATH;
				String legacyTxtPath = props.getProperty(Constants.LEGACY_TXT_PATH);
				String scriptPath = props.getProperty(Constants.NEWPARM_SCRIPT_PATH);
				String tmpKey = secret.substring(enclosingStr.length(), secret.length() - enclosingStr.length());
				String secretKey = props.getProperty(tmpKey);

				boolean foundLegacyPw = false;
				if (legacyTxtPath != null && legacyTxtPath.length() > 0) {
					String pwdConfigUser = props.getProperty(Constants.TEST_USER_ID);
					String tmp = getSecretFromFile(legacyTxtPath, secretKey, pwdConfigUser);
					if (tmp != null && tmp.length() > 0) {
						foundLegacyPw = true;
						retSecret = tmp;
					}
					logger.debug("Obtained pw based on  Constants.LEGACY_TXT_PATH {}" , pwdConfigUser);
				}
				if (!foundLegacyPw) {
					if (scriptPath != null && secretKey != null) {
						ret = HostUtils.hostExecuteGetStdOutput(scriptPath + " " + secretKey);
						if (ret != null && ret.getProcessReturnValue() == 0) {
							retSecret = ret.getStdOut();
							logger.info("Secret key with actual pwd from Shell script returned successfully for key:{}",
									secretKey);
						} else {
							logger.error("Script either returns non-zero value or returns a null Object for key:{}",
									secretKey);
							return null;
						}
					} else {
						logger.error("Either Script File path or Secret key is null");
						return null;
					}
				}
			} else {
				logger.error("Properties are empty. Not able to replace Secret key with actual Secret");
				return null;
			}
		} else {
			logger.info("DB Password found instead of Secret key. Returning it with no change.");
		}
		logger.debug("END: getSecret method to fetch decrypted secret from Shell script.");
		return retSecret;
	}

	private static String getSecretFromFile(String legacyTxtPath, String key, String pwdConfigUser) {
		if (key == null || key.length() == 0) {
			logger.error("Legacy File Path populated But Secret Key Is Null");
			return null;
		}
		Properties legacyProps = new Properties();
		loadProps(legacyProps, legacyTxtPath);
		String pw = (String) legacyProps.get(key);

		if (pw == null || pw.length() == 0) {
			logger.error("Legacy File Path populated But Secret Key Is Null");
			return null;
		}

		if (!validUser(legacyProps, key, pwdConfigUser)) {
			return null;
		}
		return pw;
	}

	private static boolean validUser(Properties legacyProps, String key, String pwdConfigUser) {
		String usersListKey = key + Constants.USER_PW_EXTENSIONS;
		String idList = (String) legacyProps.get(usersListKey);

		if (isNull(idList)) {
			logger.error("Legacy File Path populated But Users List For " + usersListKey + " Is Empty ");
			return false;
		}

		String userId = getUserName();
		if (!isNull(pwdConfigUser)) {
			userId = pwdConfigUser;
			logger.debug("got userid from pwdConfig {}" , userId);
		}
		if (isNull(userId)) {
			logger.error("System Property " + Constants.USER_ID_SYS_PROPERTY + " Is Not Populated");
			return false;
		}

		List<String> usersArray = (List<String>) Arrays.asList(idList.split(" "));
		if (usersArray.contains(userId)) {
			return true;
		}

		return false;
	}

	private static String getUserName() {
		String userId = null;
		if (isNull(userId) && System.getProperty(Constants.USER_ID_SYS_PROPERTY) != null) {
			userId = System.getProperty(Constants.USER_ID_SYS_PROPERTY);
			logger.debug("Obtained password from " + Constants.USER_ID_SYS_PROPERTY + " " + userId);
		}
		if (isNull(userId)) {
			Map<String, String> envs = System.getenv();
			userId = envs.get("LOGNAME");
			logger.debug("Obtained userId from envs.get(LOGNAME) {}" + userId);
			if (isNull(userId)) {
				userId = envs.get("USERNAME");
				logger.debug("Obtained userId from envs.get(USERNAME) {}" + userId);
			}
		}

		return userId;
	}

	private static boolean isNull(String xrefElementName) {
		return (xrefElementName == null || xrefElementName.length() == 0);
	}
}
