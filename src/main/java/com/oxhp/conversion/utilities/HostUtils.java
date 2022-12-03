package com.oxhp.conversion.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HostUtils {

	public static final Logger logger = LogManager.getLogger(HostUtils.class);

	private static String[] passwordEscapeChars = { "^", "%" };

	/**
	 * Executes a host command and returns the standard output from that command
	 * 
	 * @param command
	 *            the command to be executed
	 * @return execution return value.
	 */
	public static int hostExecute(String command) {
		int processRetVal = -10000;
		ReadStream s1 = null;
		ReadStream s2 = null;
		boolean errorOccurred = false;
		try {

			logger.info("executing host command 1 {} ", command);
			String[] execAry = command.split(" ");
			Process process = Runtime.getRuntime().exec(execAry);
			s1 = new ReadStream("stdin", process.getInputStream());
			s2 = new ReadStream("stderr", process.getErrorStream());
			s1.start();
			s2.start();
			s1.join();
			s2.join();
			processRetVal = process.waitFor();

			if (s1.getRunException() != null) {
				logger.error("Unrecoverable Error in ReadStream for stdin:" + s1.getRunException());
				errorOccurred = true;
			}
			if (s2.getRunException() != null) {
				logger.error("Unrecoverable Error in ReadStream for stderr:" + s2.getRunException());
				errorOccurred = true;
			}
			if (errorOccurred) {
				logger.error("Exception Caught in hostExecuteGetStdOutput. Check logs! ");
				throw new Exception("Exception Caught in hostExecuteGetStdOutput. Check logs! ");
			}
			logger.info("StdErr: {}", s2.getString());
			logger.info("Host command returns {}", processRetVal);
			
		} catch (Exception e) {
			logger.error("Error occured in hostExecute:{}", e);
			processRetVal = -10000;
		}
		return processRetVal;
	}

	public static HostExecuteGetStdOutput_outParm hostExecuteGetStdOutput(String command) {
		// TODO Auto-generated method stub
		return hostExecuteGetStdOutput(command, false);
	}

	/**
	 * Executes a host command and returns the standard output from that command
	 * 
	 * @param command
	 *            the command to be executed
	 * @param addNl
	 *            adds a new line after each of the process's console "read"
	 * @return an object containing the standard output/err, and execution
	 *         return value.
	 */
	public static HostExecuteGetStdOutput_outParm hostExecuteGetStdOutput(String command, boolean addNl) {
		HostExecuteGetStdOutput_outParm retObj = new HostExecuteGetStdOutput_outParm();
		int processRetVal = -10000;
		ReadStream s1 = null;
		ReadStream s2 = null;
		boolean errorOccurred = false;
		try {
			logger.info("executing host command 2 {}", command);
			String[] execAry = command.split(" ");
			Process process = Runtime.getRuntime().exec(execAry);
			s1 = new ReadStream("stdin", process.getInputStream(), addNl);
			s2 = new ReadStream("stderr", process.getErrorStream(), addNl);
			s1.start();
			s2.start();
			s1.join();
			s2.join();
			processRetVal = process.waitFor();
			retObj.setProcessReturnValue(processRetVal);
			retObj.setStdOut(s1.getString());
			retObj.setStdErr(s2.getString());
			if (s1.getRunException() != null) {
				logger.error("Unrecoverable Error in ReadStream for stdin:" + s1.getRunException());
				errorOccurred = true;
			}
			if (s2.getRunException() != null) {
				logger.error("Unrecoverable Error in ReadStream for stderr:" + s2.getRunException());
				errorOccurred = true;
			}
			if (errorOccurred) {
				logger.error("Exception Caught in hostExecuteGetStdOutput. Check logs! ");
				throw new Exception("Exception Caught in hostExecuteGetStdOutput. Check logs! ");
			}
		} catch (Exception e) {
			logger.error("Exception occurred in hostExecuteGetStdOutput:{}", e);
			retObj.setException(e);
			processRetVal = -10000;
		}
		retObj.setProcessReturnValue(processRetVal);
		return retObj;
	}

	private static String getBufferedReaderOutput(BufferedReader stdInput) {
		String s = null;
		String retVal = null;
		StringBuffer buff = new StringBuffer();
		try {
			while ((s = stdInput.readLine()) != null) {
				buff.append(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		retVal = buff.toString();
		buff = null;
		return retVal;
	}

	public static String resolvePasswordUnix(String pwString, String newparmsCommandString) throws Exception {
		return resolvePasswordUnix(pwString, newparmsCommandString, null);
	}

	public static String resolvePasswordUnix(String pwString, String newparmsCommandString, String userId)
			throws Exception {
		String resolvedPw = new String(pwString);
		logger.info("NewParms Command = {}", newparmsCommandString);
		if (checkForPasswordEscapeChars(pwString)) {
			if (newparmsCommandString == null) {
				throw new Exception("NEWPARMS_COMMAND not Supplied.");
			}
			String command = newparmsCommandString;

			String pw = pwString;
			String pwKey = pw.substring(1, pw.length() - 1);
			command = command.concat(" " + pwKey);
			HostExecuteGetStdOutput_outParm retObj = hostExecuteGetStdOutput(command);
			retObj.getProcessReturnValue();
			if (retObj.getProcessReturnValue() != 0) {
				throw new Exception("Error Obtaining Password During Host Command Execution.\n Error Is: "
						+ retObj.getException().getMessage() + "\nReturn Value Is " + retObj.getProcessReturnValue()
						+ "\n" + retObj.getStdErr());
			}
			String obtainedPassword = retObj.getStdOut();
			if (obtainedPassword != null && obtainedPassword.length() > 0) {
				resolvedPw = obtainedPassword;
			} else {
				if (userId == null)
					userId = "";
				else
					userId = userId.concat(" ");

				throw new Exception(
						"Unable To Obtain Password For " + pwKey + " User Id " + userId + "May Not Have Access");
			}
		}
		return resolvedPw;
	}

	private static boolean checkForPasswordEscapeChars(String pwString) {
		for (int idx = 0; idx < passwordEscapeChars.length; idx++) {
			if (pwString.startsWith(passwordEscapeChars[idx]) && pwString.endsWith(passwordEscapeChars[idx]))
				return true;
		}
		return false;
	}

	/**
	 * @param logMessageHolder
	 * @param testCommand
	 * @param unzipCommand
	 * @param tempDir
	 * @param sourceDirectory
	 * @param sourceFileName
	 * @return int: -1: unzip is not configured 0: successful 0: file not a zip
	 *         file 2: error during unzip occurred 3: error during rename
	 *         occurred.
	 */
	public static int performUnzip(ArrayList<String> logMessageHolder, String testCommand, String unzipCommand,
			String tempDir, String sourceDirectory, String sourceFileName) {

		int retVal = -1;
		int hostRetVal = -1;

		String command = testCommand + " " + sourceDirectory + sourceFileName;
		hostRetVal = hostExecute(command);
		if (hostRetVal != 0) {
			slog(logMessageHolder, sourceDirectory + sourceFileName + " Is Not A ZipFile");
			return 0;
		}
		slog(logMessageHolder, sourceDirectory + sourceFileName + " Is A ZipFile");

		command = unzipCommand + " " + sourceDirectory + sourceFileName + " " + tempDir;

		hostRetVal = hostExecute(command);
		String msg = null;
		retVal = hostRetVal;
		if (hostRetVal != 0) {
			switch (hostRetVal) {
			case 2:
				msg = sourceDirectory + sourceFileName + " Is NOT A ZipFile";
				slog(logMessageHolder, msg);
				break;
			case 3:
				msg = "Unable To 'mv' Unzipped File From " + tempDir + " To " + sourceDirectory + sourceFileName;
				slog(logMessageHolder, msg);
				break;
			default:
				msg = "Undefined Error Code Returned While Attempting to Unzip " + sourceDirectory + sourceFileName;
				slog(logMessageHolder, msg);
				retVal = -1;
			}

		}
		return retVal;
	}

	public static String doRegexReplacement(String theString, String expression, String replaceString,
			boolean removeAll) {
		if (removeAll) {
			return theString.replaceAll(expression, replaceString);
		} else
			return theString.replace(expression, replaceString);
	}

	private static void slog(ArrayList<String> logMessageHolder, String logString) {
		if (logMessageHolder != null)
			logMessageHolder.add(logString);
		else {
			logger.info(logString);
		}

	}
}
