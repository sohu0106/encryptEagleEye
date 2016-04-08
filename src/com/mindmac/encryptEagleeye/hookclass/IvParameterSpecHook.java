package com.mindmac.encryptEagleeye.hookclass;

import java.util.ArrayList;
import java.util.List;

import com.mindmac.encryptEagleeye.Util;


import android.os.Binder;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class IvParameterSpecHook extends MethodHook {
	private static final String mClassName = "javax.crypto.spec.SecretKeySpec";

	private IvParameterSpecHook(Methods method) {
		super(mClassName, null);
	}
	
	//public javax.crypto.spec.IvParameterSpec(byte[] key)
	//IvParameterSpec(byte[] iv)
	//IvParameterSpec(byte[] iv, int offset, int len)	
	// javax.crypto.spec.IvParameterSpec.java

	private enum Methods {
		IvParameterSpec
	};

	public static List<MethodHook> getMethodHookList() {
		List<MethodHook> methodHookList = new ArrayList<MethodHook>();
		for(Methods method : Methods.values())
			methodHookList.add(new IvParameterSpecHook(method));

		return methodHookList;
	}

	@Override
	public void after(MethodHookParam param) throws Throwable {
		int uid = Binder.getCallingUid();
		logSpecial(uid, param);
	}
	
	private void logSpecial(int uid, MethodHookParam param){
		if(!this.isNeedLog(uid))
			return;
		
		byte[] key = null;
		String k = "";
		String algorithm = "IvParameterSpec";
		
		if (param.args.length > 0 && param.args[0] != null)
		{
			key = (byte[]) param.args[0];
		}		
		
		if(key != null)
		{
			for (int i = 0; i < key.length; i++) 
			{
	    		k += (int) key[i]; 
	    		k += ", ";
	    	}
	
	    	k = k.substring(0, k.length()-2);
		}
		
		String logMsg = String.format("{\"Basic\":[\"%d\",\"%d\",\"false\"], \"CryptoUsage\": " +
				"{\"operation\": \"IvParameterSpec\", \"key\": \"%s\", \"algorithm\": \"%s\"}}", 
				uid, Util.FRAMEWORK_HOOK_SYSTEM_API, k, algorithm);
		
		Log.i(Util.LOG_TAG, logMsg);
	}
}
