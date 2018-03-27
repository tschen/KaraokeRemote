/*
Copyright (c) 2018 Tim Chen

This file is part of KaraokeRemote.

This file may be used under the terms of the MIT license.

This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Public License along with KaraokeRemote.
*/
package com.telegraphmedia.karaokeremote;

import org.teleal.cling.android.AndroidUpnpServiceConfiguration;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.transport.impl.apache.StreamClientConfigurationImpl;
import org.teleal.cling.transport.impl.apache.StreamClientImpl;
import org.teleal.cling.transport.spi.StreamClient;

import android.net.wifi.WifiManager;

public class KaraokeRemoteUpnpService extends AndroidUpnpServiceImpl {
	@Override
	protected AndroidUpnpServiceConfiguration createConfiguration(WifiManager wifiManager) {
		return new AndroidUpnpServiceConfiguration(wifiManager) {
			@Override
			public ServiceType[] getExclusiveServiceTypes() {
				return new ServiceType[] {
						new ServiceType("schemas-telegraphmedia-com", "KaraokeManager", 1)
				};
			} 

			@Override 
			public StreamClient<StreamClientConfigurationImpl> createStreamClient() { 
				return new StreamClientImpl(new StreamClientConfigurationImpl() {
					// Set 5 minute timeouts in case there is a lot of data to
					// receive from the server
					public int getConnectionTimeoutSeconds() {
						return 300; 
					} 
					public int getDataReadTimeoutSeconds() { 
						return 300; 
					} 
				}); 
			}
		};
	}
}
