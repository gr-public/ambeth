package com.koch.ambeth.xml.typehandler;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.koch.ambeth.util.exception.RuntimeExceptionUtil;
import com.koch.ambeth.xml.IReader;
import com.koch.ambeth.xml.ITypeBasedHandler;
import com.koch.ambeth.xml.IWriter;

public class InetAddressTypeHandler extends AbstractHandler implements ITypeBasedHandler {
	@Override
	public void writeObject(Object obj, Class<?> type, IWriter writer) {
		writer.writeAttribute(xmlDictionary.getValueAttribute(), ((InetAddress) obj).getHostAddress());
	}

	@Override
	public Object readObject(Class<?> returnType, Class<?> objType, int id, IReader reader) {
		try {
			return InetAddress.getByName(reader.getAttributeValue(xmlDictionary.getValueAttribute()));
		}
		catch (UnknownHostException e) {
			throw RuntimeExceptionUtil.mask(e);
		}
	}
}
