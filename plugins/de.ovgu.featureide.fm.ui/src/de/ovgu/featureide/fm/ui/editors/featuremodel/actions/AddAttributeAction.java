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
package de.ovgu.featureide.fm.ui.editors.featuremodel.actions;


import static de.ovgu.featureide.fm.core.localization.StringTable.ADD_ATTRIBUTE;
import static de.ovgu.featureide.fm.core.localization.StringTable.ADD_ATTRIBUTES;
import static de.ovgu.featureide.fm.core.localization.StringTable.DELETE_INCLUDING_SUBFEATURES;
import static de.ovgu.featureide.fm.core.localization.StringTable.FEATURE_DESCRIPTION;
import static de.ovgu.featureide.fm.core.localization.StringTable.PLEASE_ENTER_A_DESCRIPTION_FOR_FEATURE_;
import static de.ovgu.featureide.fm.core.localization.StringTable.HERE_YOU_CAN_ADD_ATTRIBUTES;

import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.event.FeatureIDEEvent;
import de.ovgu.featureide.fm.core.base.event.FeatureIDEEvent.EventType;
import de.ovgu.featureide.fm.core.functional.Functional;
import de.ovgu.featureide.fm.ui.FMUIPlugin;
import de.ovgu.featureide.fm.ui.editors.ChangeFeatureDescriptionDialog;
import de.ovgu.featureide.fm.ui.editors.AddAttributeDialog;
import de.ovgu.featureide.fm.ui.editors.featuremodel.editparts.FeatureEditPart;
import de.ovgu.featureide.fm.ui.editors.featuremodel.editparts.ModelEditPart;
import de.ovgu.featureide.fm.ui.editors.featuremodel.operations.ElementDeleteOperation;
import de.ovgu.featureide.fm.ui.editors.featuremodel.operations.FeatureTreeDeleteOperation;

/**
 * Deletes the selected features and moves their unselected children upwards. Also deletes the selected propositional constraint.
 *
 * @author Thomas Thuem
 * @author Christian Becker
 * @author Marcus Pinnecke (Feature Interface)
 */

public class AddAttributeAction extends SingleSelectionAction {

	public AddAttributeAction(Object viewer, IFeatureModel featureModel, Object graphicalViewer) {
		super(ADD_ATTRIBUTE, viewer);
	}

	@Override
	public void run() {
		String attribute = "";
		
		final AddAttributeDialog dialog =
			new AddAttributeDialog(null, ADD_ATTRIBUTES, HERE_YOU_CAN_ADD_ATTRIBUTES + feature.getName() + "'", attribute);
		dialog.open();
		
	}

	@Override
	protected void updateProperties() {
		setEnabled(true);
		setChecked(false);
	}
}
