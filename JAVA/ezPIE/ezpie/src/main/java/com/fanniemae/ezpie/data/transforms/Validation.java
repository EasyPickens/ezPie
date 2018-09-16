package com.fanniemae.ezpie.data.transforms;

import java.util.Calendar;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.transforms.validation.DataValidation;
import com.fanniemae.ezpie.data.transforms.validation.ValidateDate;
import com.fanniemae.ezpie.data.transforms.validation.ValidateList;
import com.fanniemae.ezpie.data.transforms.validation.ValidateNumeric;
import com.fanniemae.ezpie.data.transforms.validation.ValidateString;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.DataWriter;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2018-09-15
 * 
 */

public class Validation extends DataTransform {

	protected String[] _validationColumnNames = new String[] { "row", "column_name", "value_provided", "reason_failed", "date_validated" };
	protected DataType[] _validationDataTypes = new DataType[] { DataType.IntegerData, DataType.StringData, DataType.StringData, DataType.StringData, DataType.DateData };

	protected DataValidation[] _validationOperations;

	public Validation(SessionManager session, Element transform) {
		super(session, transform);
		_isolate = true;
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		throw new PieException("Validation data transform requires exclusive use of the dataset.");
	}

	@Override
	public DataStream processDataStream(DataStream inputStream, int memoryLimit) {
		// Get the name of the validation report dataset.
		String validationResults = FileUtilities.getRandomFilename(_session.getStagingPath(), ".dat");

		DataStream validationStream = null;

		try (DataReader dr = new DataReader(inputStream); DataWriter dw = new DataWriter(validationResults, memoryLimit);) {
			dw.setDataColumns(_validationColumnNames, _validationDataTypes);
			String[][] inputSchema = dr.getSchema();

			// Read the validation elements
			NodeList nl = XmlUtilities.selectNodes(_transform, "*");
			int length = nl.getLength();
			if (length == 0) {
				throw new PieException("Validation requires at least one child element.");
			}

			// Build the array of validation operations
			_validationOperations = new DataValidation[length];
			for (int i = 0; i < length; i++) {
				switch (nl.item(i).getNodeName()) {
				case "Date":
					_validationOperations[i] = new ValidateDate(_session, (Element) nl.item(i), inputSchema);
					break;
				case "Numeric":
					_validationOperations[i] = new ValidateNumeric(_session, (Element) nl.item(i), inputSchema);
					break;
				case "String":
					_validationOperations[i] = new ValidateString(_session, (Element) nl.item(i), inputSchema);
					break;
				case "List":
					_validationOperations[i] = new ValidateList(_session, (Element) nl.item(i), inputSchema);
					break;					
				default:
					throw new PieException(String.format("%s is not a currently supported validation operation.", nl.item(i).getNodeName()));
				}
			}

			// Validate the data
			int rowCount = 0;
			int problemCount = 0;
			while (!dr.eof()) {
				Object[] dataRow = dr.getDataRow();
				for (int i = 0; i < _validationOperations.length; i++) {
					Object[] validationInfo = _validationOperations[i].validateDataRow(dataRow);
					if (validationInfo != null) {
						dw.writeDataRow(validationInfo);
						problemCount++;
					}
				}
				rowCount++;
			}
			dr.close();

			Calendar calendarExpires = Calendar.getInstance();
			if (_localCacheEnabled) {
				calendarExpires.add(Calendar.MINUTE, _localCacheMinutes);
			}
			dw.setFullRowCount(rowCount); // dc.getFullRowCount(_lFullRowCount));
			dw.setBufferFirstRow(1); // dc.getBufferFirstRow());
			dw.setBufferLastRow(rowCount); // dc.getBufferLastRow());
			dw.setBufferExpires(calendarExpires.getTime());
			dw.setFullRowCountKnown(true); // dc.getFullRowCountKnown());
			dw.close();
			dr.close();
			validationStream = dw.getDataStream();
			_session.addDataSet(_name, validationStream);
			_session.addLogMessage("", "Validation Results", String.format("%,d problems found (%,d bytes in %s, args)", problemCount, validationStream.getSize(), validationStream.IsMemory() ? "memorystream" : "filestream"));
		} catch (Exception ex) {
			throw new PieException(String.format("Error while running data set validation. %s", ex.getMessage()), ex);
		}
		return inputStream;
	}

}
