package com.mindmac.encryptEagleeye.hookclass;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.Mac;

import com.mindmac.encryptEagleeye.Util;

import android.os.Binder;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class SignatureHook extends MethodHook
{
	private static final String mClassName = "java.security.Signature";
	private Methods mMethod = null;
	private static final int DECRYPT_MODE = 2;
	private static final int ENCRYPT_MODE = 1;

	private SignatureHook(Methods method) 
	{
		super(mClassName, method.name());
		mMethod = method;
	}

// 		update(byte)
//	Updates the data to be signed or verified by a byte.
//		update(byte[])
//	Updates the data to be signed or verified, using the specified array of bytes.
//		update(byte[], int, int)
//	Updates the data to be signed or verified, using the specified array of bytes, starting at the specified offset.


//	getInstance(String)
//	Generates a Signature object that implements the specified algorithm.
//	getInstance(String, String)
	
	private enum Methods {
		update,
		getInstance,
	};
	

	public static List<MethodHook> getMethodHookList() {
		List<MethodHook> methodHookList = new ArrayList<MethodHook>();
		methodHookList.add(new SignatureHook(Methods.update));		
		methodHookList.add(new SignatureHook(Methods.getInstance));
		return methodHookList;
	}
	
	private ArrayList<byte[]> extractData( byte[] bytes )
	{
		ArrayList<byte[]> dataSlices = new ArrayList<byte[]>();
		
		int byteCount = bytes.length;
		int byteOffset = 0;
		
		while( byteCount > 0 )
		{
			int targetDataLen = byteCount>Util.DATA_BYTES_TO_LOG/2 ? Util.DATA_BYTES_TO_LOG/2 : byteCount;
			byte[] targetData = new byte[targetDataLen];
			
			for( int i=0; i<targetDataLen; i++ )
			{
				targetData[i] = bytes[byteOffset+i];
			}
			
			byteOffset += targetDataLen;
			byteCount -= targetDataLen;
			dataSlices.add(targetData);
		}
		
		Log.i( Util.LOG_TAG, "in Signature()::extractData(),byteCount="+byteCount+",dataSlices="+dataSlices.toString() );
		
		return dataSlices;
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
		
		for(int i=0; i<minSize; i++)
		{
			plainText = Util.toHex(plainByteList.get(i));
			encryptText = Util.toHex(encryptByteList.get(i));
			
			String msg = String.format("{\"Basic\":[\"%d\",\"%d\",\"false\"], \"CryptoUsage\":{\"plaintext\":" +
					"\"%s\",\"encrypttext\":\"%s\",\"operation\":\"%s\",\"id\":\"%d\" }}", 
					uid, Util.FRAMEWORK_HOOK_SYSTEM_API, plainText, encryptText,  operation, id);
			
			Log.i(Util.LOG_TAG, msg);
		}
		
		Log.i( Util.LOG_TAG, "in Signature()::logMsg(),minSize="+minSize+",operation="+operation+",plainText="+plainText );
	}
	
	 
	@Override
	public void after(MethodHookParam param) throws Throwable 
	{
		int uid = Binder.getCallingUid();
		if(!Util.isAppNeedFrLog(uid))
			return;
		
		Signature sig = (Signature)param.thisObject;
		if(sig == null)
		{
//			Log.i( Util.LOG_TAG, "sig == null,return,uid="+uid );
			
			return;
		}
		
		String operationMode = "";
		ArrayList<byte[]> plainByteList = new ArrayList<byte[]>();	
		ArrayList<byte[]> encryptByteList = new ArrayList<byte[]>();
		
				
		// update(byte)
		//	Updates the data to be signed or verified by a byte.
		//		update(byte[])
		//	Updates the data to be signed or verified, using the specified array of bytes.
		//		update(byte[], int, int)		
		
		
		if( mMethod == Methods.update )
		{
			Log.i( Util.LOG_TAG, "in SignatureHook()" + "SignatureHook::update(),param.args.length="+param.args.length );
			
			if( param.args.length == 1  )
			{
				operationMode = "Signature::update()";
				encryptByteList = extractData((byte[]) param.args[0]);
				plainByteList = extractData((byte[]) param.getResult());	
				
//				Log.i( Util.LOG_TAG, "[1]Signature::update(),param.args[0]=" + new String((byte[])param.args[0]) );
//				Log.i( Util.LOG_TAG, "[1]Signature::update(),param.getResult()=" + new String((byte[])param.getResult()) );
				
			}
			else if( param.args.length == 3 )
			{
				operationMode = "Signature::update()";
				encryptByteList = extractData((byte[]) param.args[0]);
				plainByteList = extractData((byte[]) param.getResult());
				
//				Log.i( Util.LOG_TAG, "[3]Signature::update(),param.args[0]=" + new String((byte[])param.args[0]) );
//				Log.i( Util.LOG_TAG, "[3]Signature::update(),param.getResult()=" + new String((byte[])param.getResult()) );

			}
			else
			{
				operationMode = "Signature::update()";
				encryptByteList = extractData((byte[]) param.args[0]);
				plainByteList = extractData((byte[]) param.getResult());	
				
//				Log.i( Util.LOG_TAG, "[0]Signature::update(),param.args[0]=" + new String((byte[])param.args[0]) );
//				Log.i( Util.LOG_TAG, "[0]Signature::update(),param.getResult()=" + new String((byte[])param.getResult()) );
			}
		}
		else if( mMethod == Methods.getInstance )
		{
			Log.i( Util.LOG_TAG, "in SignatureHook()" + "SignatureHook::getInstance(),param.args.length="+param.args.length );
			
			if( param.args.length==1 || param.args.length==2 )
			{
				operationMode = "Signature::getInstance()";
				encryptByteList = extractData((byte[]) param.args[0]);
				plainByteList = extractData((byte[]) param.getResult());	
			}
			else
			{
				operationMode = "Signature::getInstance()";
				encryptByteList = extractData((byte[]) param.args[0]);
				plainByteList = extractData((byte[]) param.getResult());
			}
		}
		else
		{
			Log.i( Util.LOG_TAG, "in SignatureHook()" + "SignatureHook::unkown(),param.args.length="+param.args.length );			
		}
		
		logMsg(plainByteList, encryptByteList, operationMode,  uid);
	}
}
