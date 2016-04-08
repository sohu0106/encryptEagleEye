package com.mindmac.encryptEagleeye.hookclass;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;

import com.mindmac.encryptEagleeye.Util;

import android.os.Binder;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class MD5Hook extends MethodHook {
	private static final String mClassName = "java.security.MessageDigest";
	private Methods mMethod = null;
	private static final int DECRYPT_MODE = 2;
	private static final int ENCRYPT_MODE = 1;

	private MD5Hook(Methods method) {
		super(mClassName, method.name());
		mMethod = method;
	}

	// public final byte[] doFinal(byte[] input)
	// public final byte[] doFinal(byte[] input, int inputOffset, int inputLen)
	// public final int doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset)
	// public final int doFinal(ByteBuffer input, ByteBuffer output)
	// /libcore/luni/src/main/java/javax/crypto/Cipher.java

	private enum Methods {
		digest,
		update,
	};
	

	public static List<MethodHook> getMethodHookList() {
		List<MethodHook> methodHookList = new ArrayList<MethodHook>();
		methodHookList.add(new MD5Hook(Methods.digest));
		methodHookList.add(new MD5Hook(Methods.update));
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
			cipherClass = (Class<Cipher>) Class.forName("javax.crypto.Cipher");
			mode = cipherClass.getDeclaredField("mode");
			mode.setAccessible(true);
			operationMode = (Integer) mode.get(cipherInst);
			} catch (Exception e) {
				e.printStackTrace();
			}
					
		return operationMode;
	}
	
	private void logMsg(ArrayList<byte[]> plainByteList, String operationMode, int uid)
	{
		int plainByteListSize = plainByteList.size();	
		
		int minSize = plainByteListSize;
		int maxSize = plainByteListSize;
		String plainText = "";
		String encryptText = "";
		String operation = operationMode;	
		int id = Util.getTimeId();
		
		for(int i=0; i<minSize; i++){
			plainText = Util.toHex(plainByteList.get(i));
			
			String msg = String.format("{\"Basic\":[\"%d\",\"%d\",\"false\"], \"CryptoUsage\":{\"plaintext\":" +
					"\"%s\",\"operation\":\"%s\",\"id\":\"%d\" }}", 
					uid, Util.FRAMEWORK_HOOK_SYSTEM_API, plainText,  operation, id);
			
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
		
		MessageDigest messageDigest = (MessageDigest)param.thisObject;
		if(messageDigest == null)
			return;
		
		String operationMode = "";
		ArrayList<byte[]> plainByteList = new ArrayList<byte[]>();		
		
		if( mMethod == Methods.digest )
		{
			operationMode = "MD5::digest()";
			plainByteList = extractData((byte[]) param.getResult());	
		}
		else if( mMethod == Methods.update )
		{	
			operationMode = "MD5::update()";
			if( param.args.length == 1 )
			{
				plainByteList = extractData((byte[]) param.args[0]);
			}
		}	
		
		logMsg(plainByteList, operationMode,  uid);
	}
}
