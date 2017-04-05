/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */

import java.util.List;

import interfaces.IMessage;
import loader.PluginLoader;

/**
 * A black-box framework to print different Hello World messages.
 * 
 * Hello World example for FeatureIDE projects using black-box frameworks with
 * plug-ins.
 * 
 * @author Thomas Thuem
 */
public class HelloWorld {

	public static void main(String[] args) throws InterruptedException {
		List<IMessage> messagePlugins = PluginLoader.load(IMessage.class);
		print(messagePlugins, true);
		print(messagePlugins, false);
	}

	private static void print(List<IMessage> messagePlugins, boolean startMessage) {
		for (IMessage message : messagePlugins) {
			if (message.isStartMessage() == startMessage) {
				System.out.print(message.getMessage());
			}
		}
	}

}
