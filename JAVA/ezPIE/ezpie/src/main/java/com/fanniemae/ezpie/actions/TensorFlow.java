/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.actions;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.DataWriter;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-08-22
 * 
 */

public class TensorFlow extends Action {
	protected boolean _isInternal = false;
	
	protected int _inputLength = 0;
	protected int[] _inputColumnIndexes = null;

	public TensorFlow(SessionManager session, Element action) {
		super(session, action, true);
		_isInternal = StringUtilities.toBoolean(optionalAttribute("Internal","False"));
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		String modelBundlePath = requiredAttribute("ModelBundlePath");
		if (FileUtilities.isInvalidDirectory(modelBundlePath)) {
			throw new RuntimeException(String.format("ModelBundlePath (%s) does not exist.", modelBundlePath));
		}

		String modelBundleTags = requiredAttribute("ModelBundleTags");
		String feedOperationName = requiredAttribute("FeedOperationName");
		String fetchOperationName = requiredAttribute("FetchOperationName");
		String inputDataSetName = requiredAttribute("InputDataSetName");
		String inputDataSetType = optionalAttribute("InputDataSetType", "Float");

		float[][] data = null;
		switch (inputDataSetType.toLowerCase()) {
		case "float":
			if ("bobo".equals(inputDataSetName)) {
				float[] inputfloat = { 11.7596938f, 4.0f, 0.0f, 4.0f, 2.0f };
				data = new float[1][];
				data[0] = inputfloat;
			} else {
				data = convertDataToFloatArray(inputDataSetName);
			}
			break;
		default:
			throw new RuntimeException(String.format("%s is not a currently supported type.", inputDataSetType));
		}
		
		if ((data == null) || (data.length == 0) || (data[0].length == 0)) {
			throw new RuntimeException(String.format("Input dataset %s is empty.",inputDataSetName));
		}

		try (SavedModelBundle b = SavedModelBundle.load(modelBundlePath, modelBundleTags)) {
			Session sess = b.session();
			Tensor inputTensor = Tensor.create(data);

			Tensor result = sess.runner().feed(feedOperationName, inputTensor)
					.fetch(fetchOperationName)
					.run().get(0);

			long[] shape = result.shape();
			float[][] vector = null;
			float bestPrediction = 0;
			int bestItemNumber = -1;
			if (shape.length == 1) {
				vector = new float[1][(int)shape[0]];
				vector[0] = result.copyTo(vector[0]);
				bestPrediction = vector[0][0];
				bestItemNumber = 0;
			} else if (shape.length == 2) {
				vector = new float[(int)shape[0]][(int)shape[1]];
				vector = result.copyTo(vector);
				
				int itemNumber = 0;
				for (float val : vector[0]) {
					if (val > bestPrediction) {
						bestItemNumber = itemNumber;
						bestPrediction = val;
					}
					itemNumber++;
				}				
			}

			if (bestItemNumber > -1) {
				_session.addToken("TensorFlow", "Prediction", new BigDecimal(bestPrediction).toPlainString());
				_session.addToken("TensorFlow", "ItemNumber", String.format("%d", bestItemNumber));
			}
			String dataFilename = FileUtilities.getRandomFilename(_session.getStagingPath(), "dat");
			try (DataWriter dw = new DataWriter(dataFilename, _session.getMemoryLimit(), false)) {
				String[][] schema = new String[][] { { "item", "String" }, { "value", "Double" } };
				dw.setDataColumns(schema);
				int rowCount = vector[0].length;
				for (int i = 0; i < rowCount; i++) {
					Object[] dataRow = new Object[] { i, (double) vector[0][i] };
					dw.writeDataRow(dataRow);
				}
				Calendar calendarExpires = Calendar.getInstance();
				if (_session.cachingEnabled())
					calendarExpires.add(Calendar.MINUTE, _session.getCacheMinutes());
				dw.setFullRowCount(rowCount);
				dw.setBufferFirstRow(1);
				dw.setBufferLastRow(rowCount);
				dw.setBufferExpires(calendarExpires.getTime());
				dw.setFullRowCountKnown(true);
				dw.close();
				DataStream dataStream = dw.getDataStream();
				dataStream.setInternal(_isInternal);
				_session.addDataSet(_name, dataStream);
				_session.addLogMessage("", "Data Returned", String.format("%,d rows (%,d bytes in %s)", rowCount, dataStream.getSize(), dataStream.IsMemory() ? "memorystream" : "filestream"));
			} catch (IOException e) {
				throw new RuntimeException(String.format("Error while saving model results. %s", e.getMessage()), e);
			}
		}

		return null;
	}

	protected float[][] convertDataToFloatArray(String dataSetName) {
		float[][] data = null;
		DataStream ds = _session.getDataStream(dataSetName);
		try (DataReader dr = new DataReader(ds)) {
			defineInputColumns(dr.getColumnNames());

			Object[] dataRow = null;
			float[] row = null;
			data = new float[(int) dr.getFullRowCount()][];
			int rowNumber = 0;
			while (!dr.eof()) {
				dataRow = dr.getDataRow();
				row = new float[_inputLength];
				for (int i = 0; i < row.length; i++) {
					Object value = dataRow[_inputColumnIndexes[i]];
					if (value == null) {
						throw new RuntimeException("Input data value is null.");
					} else if (value.getClass().getName().indexOf("Double") > -1) {
						row[i] = (float) ((Double) dataRow[_inputColumnIndexes[i]]).doubleValue();
					} else if (value.getClass().getName().indexOf("double") > -1) {
						row[i] = (float) ((double) dataRow[_inputColumnIndexes[i]]);						
					} else if (value.getClass().getName().indexOf("Integer") > -1) {
						row[i] = (float) ((Integer) dataRow[_inputColumnIndexes[i]]).doubleValue();
					} else if (value.getClass().getName().indexOf("integer") > -1) {
						row[i] = (float) ((int) dataRow[_inputColumnIndexes[i]]);						
					} else {
						row[i] = (float) dataRow[_inputColumnIndexes[i]];
					}
				}
				data[rowNumber] = row;
				rowNumber++;
			}
		} catch (IOException e) {
			throw new RuntimeException(String.format("Error reading input data. %s", e.getMessage()), e);
		} catch (Exception e) {
			throw new RuntimeException(String.format("Error reading or covernt input data. %s", e.getMessage()), e);
		}
		return data;
	}

	protected void defineInputColumns(String[] fileColumns) {
		List<String> inputColumnNames = Arrays.asList(fileColumns);

		NodeList inputColumnNodes = XmlUtilities.selectNodes(_action, "InputColumn");
		int numberOfInputColumns = inputColumnNodes.getLength();

		if (numberOfInputColumns > 0) {
			_inputColumnIndexes = new int[numberOfInputColumns];

			for (int i = 0; i < numberOfInputColumns; i++) {
				Element columnElement = (Element) inputColumnNodes.item(i);

				String inputName = _session.getAttribute(columnElement, "Name");

				_inputColumnIndexes[i] = inputColumnNames.indexOf(inputName);
				if (_inputColumnIndexes[i] == -1) {
					throw new RuntimeException(String.format("Column %s not found in the input data set.", inputName));
				}
			}
		} else {
			numberOfInputColumns = inputColumnNames.size();
			_inputColumnIndexes = new int[numberOfInputColumns];

			for (int i = 0; i < numberOfInputColumns; i++) {
				_inputColumnIndexes[i] = i;
			}
		}
		_inputLength = _inputColumnIndexes.length;
	}

}
