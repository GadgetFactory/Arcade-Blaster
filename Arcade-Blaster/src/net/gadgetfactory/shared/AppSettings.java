/*
  Part of the Papilio Java Utility Library

  Copyright (c) 2010-12 GadgetFactory LLC

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package net.gadgetfactory.shared;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Properties;

public class AppSettings
{
	// We cannot use a name such as ".Gadgetfactory" because it is not legal in Windows
	private final String COMPANY_FOLDER = "Gadgetfactory";
	
	private Properties settings;

	private File settingsPath, preferencesFile;

	public boolean isFilePresent() {
		return preferencesFile.isFile();
	}

	public File getSettingsPath() {
		// Do not return settingsPath - otherwise that would break data encapsulation, 
		// rather return a clone of settingsPath.
		return new File(settingsPath.getAbsolutePath());
	}

	public AppSettings(String sSettingsFolder, String sPreferencesFile, 
					   boolean runningonWindows)
	{
		File companyFolder;
		
		if (runningonWindows) {
			companyFolder = new File(System.getenv("APPDATA"), COMPANY_FOLDER);
			settingsPath = new File(companyFolder, sSettingsFolder);
		}
		else {
			// Prefix folder names with "." so as to hide it under Linux.
			companyFolder = new File(System.getProperty("user.home"), "." + COMPANY_FOLDER);
			settingsPath = new File(companyFolder, "." + sSettingsFolder);
		}
		
		preferencesFile = new File(settingsPath, sPreferencesFile);
	}
	
	
	public String getStringProperty(String sName) {
		return settings.getProperty(sName);
	}

	public void setProperty(String sName, String sValue) {
		settings.setProperty(sName, sValue);
	}


	public void Cache(Properties defaultSettings)
	{
		InputStreamReader isr = null;

		settings = new Properties(defaultSettings);		// Initialize from defaults.
		
		if (!EnsureSettingsFolder())
		// => Settings folder does not exist, so exit.
			return;
		else if (!preferencesFile.isFile())
		// => Settings folder exists, but preferences file does not exist, so exit.
			return;
		
		try {
			isr = new InputStreamReader(new FileInputStream(preferencesFile), "UTF-8");
			settings.load(isr);
		}
		catch (IOException e) {
			System.err.println("AppSettings.Cache()\t" + e.getMessage());
		}
		finally
		{
			if (isr != null) {
				try {
					isr.close();  
				}
				catch (IOException ioex) {
					System.err.println(ioex.getMessage());
				}
			}
		}
		
	}
	
	public void Save()
	{
		OutputStreamWriter osw = null;
		
		if (!EnsureSettingsFolder())
		// => Settings folder does not exist, so exit.
			return;

		try {
			osw = new OutputStreamWriter(new FileOutputStream(preferencesFile), "UTF-8");
			settings.store(osw, null);
		}
		catch (IOException e) {
			System.err.println("AppSettings.Save()\t" + e.getMessage());
		}
		finally
		{
			if (osw != null) {
				try {
					osw.close();  
				}
				catch (IOException ioex) {
					System.err.println(ioex.getMessage());
				}
			}
		}
		
	}
	

	public boolean EnsureSettingsFolder()
	{
		if (!settingsPath.isDirectory()) {
/*	------------------------------------------------------------------------------------
 * 	File.mkdirs()
 * 		Creates the directory named by this abstract pathname, including any necessary but 
 * 		nonexistent parent directories. Note that if this operation fails it may have 
 * 		succeeded in creating some of the necessary parent directories.
 * 		Returns true if and only if the directory was created, along with ALL necessary 
 * 		parent directories; false otherwise.  
 *	------------------------------------------------------------------------------------ */
			if (settingsPath.mkdirs())
				return true;
			else
		    // => There was an error creating Settings folder. (No Exception is thrown).
				return false;
		}
		else
			return true;
	}
	
}
