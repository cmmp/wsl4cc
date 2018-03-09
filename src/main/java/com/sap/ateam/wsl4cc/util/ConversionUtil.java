package com.sap.ateam.wsl4cc.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.sap.conn.jco.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.ateam.wsl4cc.Wsl4ccException;

public class ConversionUtil {

	public static Object convertJCoFieldToPrimitive(JCoField field) {
		Object ret = null;
		int type = field.getType();

		switch (type) {
			case JCoMetaData.TYPE_CHAR:
			case JCoMetaData.TYPE_INT1:
			case JCoMetaData.TYPE_INT2:
			case JCoMetaData.TYPE_INT8:
			case JCoMetaData.TYPE_INT:
			case JCoMetaData.TYPE_NUM:
			case JCoMetaData.TYPE_BCD:
			case JCoMetaData.TYPE_FLOAT:
			case JCoMetaData.TYPE_STRING:
			case JCoMetaData.TYPE_DECF16:
			case JCoMetaData.TYPE_DECF34:
				ret = field.getValue();
				break;

			case JCoMetaData.TYPE_BYTE:
			case JCoMetaData.TYPE_XSTRING:
			case JCoMetaData.TYPE_DATE:
			case JCoMetaData.TYPE_TIME:
				ret = field.getString();
				break;

			case JCoMetaData.TYPE_STRUCTURE:
				ret = field.getStructure();
				break;

			case JCoMetaData.TYPE_TABLE:
				ret = field.getTable();
				break;

			default:
				ret = null;
				logger.error("Unrecognized type " + field.getClassNameOfValue() + " for field " + field.getName());
				break;
		}

		return ret;
	}

	/**
	 * Convert a user provided List to a JCoTable input table.
	 *
	 * @param tableName Name of input table parameter
	 * @param table JCoTable
	 * @param list User provided list
	 * @return Resulting table
	 * @throws Wsl4ccException
	 */
	public static JCoTable convertToJCoTable(String tableName, JCoTable table, List<?> list) throws Wsl4ccException {
		if (list == null || table == null)
			return table;

		JCoMetaData meta = table.getMetaData();
		for (Object o : list) {
			if (!(o instanceof Map<?,?>))
				throw new Wsl4ccException("User input for table " + tableName + " is not a valid map.");

			Map<?,?> map = (Map<?,?>) o;
			table.appendRow();
			for (Map.Entry<?,?> e : map.entrySet()) {
				String fieldName = (String) e.getKey();
				Object fieldValue = (Object) e.getValue();

				if (meta.hasField(fieldName))
					convertPrimitiveToJCoField(table, fieldName, fieldValue);
				else
					throw new Wsl4ccException("Unrecognized or invalid field " + fieldName + " in table " + tableName);
			}
		}

		return table;
	}

	public static void convertPrimitiveToJCoField(JCoRecord record, String name, Object value) throws Wsl4ccException {
		JCoField field = record.getField(name);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd"); // TODO: Make dateFormat a query parameter.
		df.setLenient(false);

		switch (field.getType()) {
			case JCoMetaData.TYPE_DATE:
				try {
					Date d = df.parse((String) value);
					record.setValue(name, d);
				} catch (ClassCastException | ParseException pe) {
					String m = "Cannot convert a value of '" + value + "' from type " + value.getClass() + " to " + field.getTypeAsString() + " at field " + field.getName();
					throw new Wsl4ccException(m);
				}
				break;
			case JCoMetaData.TYPE_STRUCTURE:
				JCoStructure structure = field.getStructure();
				JCoRecordFieldIterator it = structure.getRecordFieldIterator();
				AbstractMap map = (AbstractMap) value;

				while (it.hasNextField()) {
					JCoRecordField structField = it.nextRecordField();
					String fname = structField.getName();
					if (map.containsKey(fname))
						structure.setValue(fname, map.get(fname));
				}
				record.setValue(name, structure);
				break;
			default:
				try {
					record.setValue(name, value);
				} catch (ConversionException ce) {
					// The ConversionException has a good message that we can reuse for our purpose.
					// E.g.: Cannot convert a value of 'ABCD' from type java.lang.String to INT at field MAX_CNT
					throw new Wsl4ccException(ce.getMessage());
				}
				break;
		}
	}

	public static Map<String,Object> convertParameterListToMap(JCoParameterList pList) {
		if (pList == null) return null;

        Map <String,Object> pMap = new HashMap<>();
        JCoParameterFieldIterator iterator = pList.getParameterFieldIterator();

        while (iterator.hasNextField()) {
        	JCoParameterField field = iterator.nextParameterField();
			Object value = convertJCoFieldToPrimitive(field);
        	pMap.put(field.getName(), value);
        }

        return pMap;
	}

    private static Logger logger = LoggerFactory.getLogger(ConversionUtil.class);

}
