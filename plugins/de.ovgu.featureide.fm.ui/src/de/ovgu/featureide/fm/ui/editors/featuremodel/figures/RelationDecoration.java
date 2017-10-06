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
package de.ovgu.featureide.fm.ui.editors.featuremodel.figures;

import java.util.List;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;

import de.ovgu.featureide.fm.ui.editors.FeatureUIHelper;
import de.ovgu.featureide.fm.ui.editors.IGraphicalFeature;
import de.ovgu.featureide.fm.ui.editors.IGraphicalFeatureModel;
import de.ovgu.featureide.fm.ui.editors.featuremodel.GUIDefaults;
import de.ovgu.featureide.fm.ui.properties.FMPropertyManager;

/**
 * A decoration for a feature connection that indicates its group type.
 *
 * @author Thomas Thuem
 * @author Stefanie Schober
 * @author Jann-Ole Henningson
 */
public class RelationDecoration extends ConnectionDecoration implements GUIDefaults {

	private final boolean fill;
	private Point lastCenter;
	private Point referencePoint;
	private IGraphicalFeature lastChild;
	private List<IGraphicalFeature> children;
	private IGraphicalFeatureModel featureModel;
	private int thresholdAngleMax = 25;
	private int thresholdAngleMin = 2;
	
	public RelationDecoration(final boolean fill, final IGraphicalFeature lastChild) {
		this.fill = fill;
		this.lastChild = lastChild;
		if (lastChild == null) {
			children = null;
		} else {
			children = FeatureUIHelper.getGraphicalSiblings(lastChild);
		}
		setForegroundColor(FMPropertyManager.getDecoratorForegroundColor());
		setBackgroundColor(FMPropertyManager.getDecoratorForegroundColor());
		setSize(TARGET_ANCHOR_DIAMETER, TARGET_ANCHOR_DIAMETER);
		if (lastChild != null) {
			featureModel = lastChild.getGraphicalModel();
		}
		lastCenter = getBounds().getLeft();
	}

	@Override
	public void setLocation(final Point p) {
		if (this instanceof LegendRelationDecoration) {
			super.setLocation(p.translate((-getBounds().width >> 1) + 1, 0));
		} else {
			if (FeatureUIHelper.hasVerticalLayout(featureModel)) {
				setSize(calculateDecorationSize());
				super.setLocation(p.translate(0, (-getBounds().width >> 1)));
			} else {
				setSize(TARGET_ANCHOR_DIAMETER, TARGET_ANCHOR_DIAMETER);
				super.setLocation(p.translate((-getBounds().width >> 1), 0));
			}
		}
	}
	
	public Dimension calculateDecorationSize() {
		double minAngle = Double.MAX_VALUE;
		double maxAngle = Double.MIN_VALUE;
		int min = Integer.MAX_VALUE;
		
		if ((children != null) && (children.size() > 1)) {
			for (final IGraphicalFeature curChild : children) {
				min = curChild.getLocation().x < min ? curChild.getLocation().x : min;			
				final Point featureLocation = FeatureUIHelper.getSourceLocation(curChild);
				final double currentAngle = calculateAngle(getBounds().getLeft(), featureLocation);
				if (currentAngle < minAngle) {
					minAngle = currentAngle;
				}
				if (currentAngle > maxAngle) {
					maxAngle = currentAngle;
				}
			}
		}
		
		double angle = maxAngle - minAngle;
		int distance = Math.abs(getBounds().getLeft().x - min);
		
		if (angle <= thresholdAngleMax && angle > thresholdAngleMin) {
			int size = TARGET_ANCHOR_DIAMETER + 
					(int)((double)Math.abs(TARGET_ANCHOR_DIAMETER - distance) / (angle - thresholdAngleMin));
			if(size % 2 == 1){
				size -= 1;
			}
			return new Dimension(size, size);
		} else if (angle <= thresholdAngleMin) {
			return new Dimension(distance, distance);
		}
		return new Dimension(TARGET_ANCHOR_DIAMETER, TARGET_ANCHOR_DIAMETER);
	}

	@Override
	public void setReferencePoint(final Point p) {
		referencePoint = p;
	}

	@Override
	protected void fillShape(final Graphics graphics) {}

	@Override
	protected void outlineShape(final Graphics graphics) {
		drawShape(graphics);
	}

	private void drawShape(final Graphics graphics) {
		if (getActiveReason() != null) {
			final Color reasonColor = FMPropertyManager.getReasonColor(getActiveReason());
			graphics.setForegroundColor(reasonColor);
			graphics.setBackgroundColor(reasonColor);
			graphics.setLineWidth(FMPropertyManager.getReasonLineWidth(getActiveReason()));
		}

		boolean verticalLayout = false;
		if (featureModel != null) {
			verticalLayout = FeatureUIHelper.hasVerticalLayout(featureModel);
		}
		double minAngle = Double.MAX_VALUE;
		double maxAngle = Double.MIN_VALUE;
		final Rectangle r;
		if (verticalLayout) {
			r = new Rectangle(getBounds()).translate((-getBounds().width >> 1), 0).shrink(1, 1);
		} else {
			r = new Rectangle(getBounds()).translate(0, (-getBounds().height >> 1)).shrink(1, 1);
		}
		Point center = verticalLayout ? getBounds().getLeft() : getBounds().getTop();
		lastCenter = center;
		
		if (Math.abs(center.y - lastCenter.y) == 1)
			center = new Point(center.x, center.y + 1);
		
		if (this instanceof LegendRelationDecoration) {
			maxAngle = calculateAngle(center, getFeatureLocation());
			minAngle = calculateAngle(center, referencePoint);
		} else {
			if ((children != null) && (children.size() > 1)) {
				for (final IGraphicalFeature curChild : children) {
					lastChild = curChild;
					final Point featureLocation = FeatureUIHelper.getSourceLocation(curChild);
					final double currentAngle = calculateAngle(center, featureLocation);
					if (currentAngle < minAngle) {
						minAngle = currentAngle;
					}
					if (currentAngle > maxAngle) {
						maxAngle = currentAngle;
					}
				}
			} else {
				return;
			}
		}

		if (fill) {
			Draw2dHelper.fillArc(graphics, r, (int) minAngle, (int) (maxAngle - minAngle));
		} else {
			graphics.drawArc(r, (int) minAngle, (int) (maxAngle - minAngle));
		}
	}

	protected Point getFeatureLocation() {
		return FeatureUIHelper.getSourceLocation(lastChild);
	}

	protected int getTargetAnchorDiameter() {
		return TARGET_ANCHOR_DIAMETER;
	}

	private double calculateAngle(final Point point, final Point referencePoint) {
		final int dx = referencePoint.x - point.x;
		final int dy = referencePoint.y - point.y;
		return 360 - Math.round((Math.atan2(dy, dx) / Math.PI) * 180);
	}
}
