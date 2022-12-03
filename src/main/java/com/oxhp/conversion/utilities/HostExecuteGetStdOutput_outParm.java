package com.oxhp.conversion.utilities;

public class HostExecuteGetStdOutput_outParm {
	private int processReturnValue = -10000;
	private String stdOut = "n/a";
	private String stdErr = "n/a";
	private Exception exception = new Exception("N/A");
	
	public int getProcessReturnValue() {
		return processReturnValue;
	}
	public void setProcessReturnValue(int processReturnValue) {
		this.processReturnValue = processReturnValue;
	}
	public String getStdErr() {
		return stdErr;
	}
	public void setStdErr(String stdErr) {
		this.stdErr = stdErr;
	}
	public String getStdOut() {
		return stdOut;
	}
	public void setStdOut(String stdOut) {
		this.stdOut = stdOut;
	}
	public void setException(Exception exception) {
		this.exception = exception;
		
	}
	public Exception getException() {
		return exception;
	}

	public String toString(){
	
		return "processReturnValue " + processReturnValue
				+ "\nstdOut " + stdOut
				+ "\nstdErr " + stdErr
				+ "\nException " + exception;
	}
}
