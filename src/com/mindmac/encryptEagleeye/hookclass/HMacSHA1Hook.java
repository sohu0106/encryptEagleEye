package com.mindmac.encryptEagleeye.hookclass;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.Mac;

import com.mindmac.encryptEagleeye.Util;

import android.os.Binder;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class HMacSHA1Hook extends MethodHook {
	private static final String mClassName = "javax.crypto.Mac";
	private Methods mMethod = null;
	private static final int DECRYPT_MODE = 2;
	private static final int ENCRYPT_MODE = 1;

	private HMacSHA1Hook(Methods method) {
		super(mClassName, method.name());
		mMethod = method;
	}

	// public final byte[] doFinal(byte[] input)
	// public final byte[] doFinal(byte[] output, int outOffset)
	//
	// init(Key key)
	// /libcore/luni/src/main/java/javax/crypto/Cipher.java
	
	//static Mac	getInstance(String algorithm)
	//static Mac 	getInstance(String algorithm, Provider provider)
	//static Mac 	getInstance(String algorithm, String provider)
	//Returns a Mac object that implements the specified MAC algorithm.

	private enum Methods {
		doFinal,
		getInstance,
	};
	

	public static List<MethodHook> getMethodHookList() {
		List<MethodHook> methodHookList = new ArrayList<MethodHook>();
		methodHookList.add(new HMacSHA1Hook(Methods.doFinal));		
		methodHookList.add(new HMacSHA1Hook(Methods.getInstance));	
		return methodHookList;
	}
	
	private ArrayList<byte[]> extractData(byte[] bytes){
		ArrayList<byte[]> dataSlices = new ArrayList<byte[]>();
		
		int byteCount = bytes.length;
		int byteOffset = 0;
		
		while(byteCount>0){
			int targetDataLen = byteCount>Util.DATA_BYTES_TO_LOG/2 ? Util.DATA_BYTES_TO_LOG/2 : byteCount;
			byte[] targetData = new byte[targetDataLen];
			for(int i=0; i<targetDataLen; i++)
				targetData[i] = bytes[byteOffset+i];
			byteOffset += targetDataLen;
			byteCount -= targetDataLen;
			dataSlices.add(targetData);
		}
		
		return dataSlices;
	}

	
	@SuppressWarnings("unchecked")
	private int getOperationMode(Cipher cipherInst){
		int operationMode = -1;
		
		Class<Cipher> cipherClass = null;
		Field mode = null;
		
		try {
			cipherClass = (Class<Cipher>) Class.forName("javax.crypto.Mac");
			mode = cipherClass.getDeclaredField("mode");
			mode.setAccessible(true);
			operationMode = (Integer) mode.get(cipherInst);
			} catch (Exception e) {
				e.printStackTrace();
			}
					
		return operationMode;
	}
	
	private void logMsg(ArrayList<byte[]> plainByteList, ArrayList<byte[]> encryptByteList,
			String operationMode, int uid)
	{
		int plainByteListSize = plainByteList.size();	
		int encryptByteListSize = encryptByteList.size();
		
		int minSize = plainByteListSize >= encryptByteListSize ? encryptByteListSize : plainByteListSize;
		int maxSize = plainByteListSize >= encryptByteListSize? plainByteListSize : encryptByteListSize;
		
		String plainText = "";
		String encryptText = "";
		String operation = operationMode;	
		int id = Util.getTimeId();
		
		for(int i=0; i<minSize; i++){
			plainText = Util.toHex(plainByteList.get(i));
			encryptText = Util.toHex(encryptByteList.get(i));
			
			String msg = String.format("{\"Basic\":[\"%d\",\"%d\",\"false\"], \"CryptoUsage\":{\"plaintext\":" +
					"\"%s\",\"encrypttext\":\"%s\",\"operation\":\"%s\",\"id\":\"%d\" }}", 
					uid, Util.FRAMEWORK_HOOK_SYSTEM_API, plainText, encryptText, operation, id);
			
			Log.i(Util.LOG_TAG, msg);
		}
	}
	
	
	private void logMsg_bp(ArrayList<byte[]> plainByteList, ArrayList<byte[]> encryptByteList,
			int operationMode, String algorithm, int uid){
		int plainByteListSize = plainByteList.size();
		int encryptByteListSize = encryptByteList.size();
		
		int minSize = plainByteListSize >= encryptByteListSize ? encryptByteListSize : plainByteListSize;
		int maxSize = plainByteListSize >= encryptByteListSize? plainByteListSize : encryptByteListSize;
		String plainText = "";
		String encryptText = "";
		String operation = "encrypt";
		if(operationMode == DECRYPT_MODE)
			operation = "decrypt";
		int id = Util.getTimeId();
		
		for(int i=0; i<minSize; i++){
			plainText = Util.toHex(plainByteList.get(i));
			encryptText = Util.toHex(encryptByteList.get(i));
			String msg = String.format("{\"Basic\":[\"%d\",\"%d\",\"false\"], \"CryptoUsage\":{\"plaintext\":" +
					"\"%s\",\"encrypttext\":\"%s\", \"operation\":\"%s\",\"algorithm\":\"%s\",\"id\":\"%d\" }}", 
					uid, Util.FRAMEWORK_HOOK_SYSTEM_API, plainText, encryptText, operation, algorithm, id);
			Log.i(Util.LOG_TAG, msg);
		}
		
		for(int i=minSize; i<maxSize; i++){
			if(i >= plainByteListSize)
				plainText = "";
			else
				plainText = Util.toHex(plainByteList.get(i));
			
			if(i >= encryptByteListSize)
				encryptText = "";
			else
				encryptText = Util.toHex(encryptByteList.get(i));
			String msg = String.format("{\"Basic\":[\"%d\",\"%d\",\"false\"], \"CryptoUsage\":{\"plaintext\":" +
					"\"%s\",\"encrypttext\":\"%s\", \"operation\":\"%s\",\"algorithm\":\"%s\",\"id\":\"%d\" }}", 
					uid, Util.FRAMEWORK_HOOK_SYSTEM_API, plainText, encryptText, operation, algorithm, id);
			Log.i(Util.LOG_TAG, msg);
		}
	}
	
	@Override
	public void after(MethodHookParam param) throws Throwable 
	{
		int uid = Binder.getCallingUid();
		if(!Util.isAppNeedFrLog(uid))
			return;
		
		Mac mac = (Mac)param.thisObject;
		if(mac == null)
			return;
		
		String operationMode = "unkown()";
		ArrayList<byte[]> plainByteList = new ArrayList<byte[]>();	
		ArrayList<byte[]> encryptByteList = new ArrayList<byte[]>();
		
				
		// public final byte[] doFinal(byte[] input)
		// public final byte[] doFinal(byte[] output, int outOffset)
		if( mMethod == Methods.doFinal )
		{
			if( param.args.length==1 || param.args.length==2 )
			{
				operationMode = "doFinal()";
				encryptByteList = extractData((byte[]) param.args[0]);
				plainByteList = extractData((byte[]) param.getResult());	
			}
			else
			{
				operationMode = "doFinal()";
				plainByteList = extractData((byte[]) param.getResult());
			}
			
		}
		else if( mMethod == Methods.getInstance )
		{
			if( param.args.length==1 || param.args.length==2 )
			{
				operationMode = "getInstance()";
				encryptByteList = extractData((byte[]) param.args[0]);
				plainByteList = extractData((byte[]) param.getResult());	
			}
			else
			{
				operationMode = "getInstance()";
				encryptByteList = extractData((byte[]) param.args[0]);
				plainByteList = extractData((byte[]) param.getResult());
			}
		}
		
		logMsg(plainByteList, encryptByteList, operationMode, uid);
	}
}
