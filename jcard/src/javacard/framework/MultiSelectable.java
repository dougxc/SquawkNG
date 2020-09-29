/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * @(#)MultiSelectable.java	1.14 02/04/01
 */

package javacard.framework;

/**
 * The <CODE>MultiSelectable</CODE> interface serves to identify the implementing
 * Applet subclass as being capable of concurrent selections.
 * A multiselectable applet is a subclass of <CODE>javacard.framework.Applet</CODE>
 * which directly or indirectly implements this interface. All applets within a
 * applet package must be multiselectable or none at all.
 * An instance of a multiselectable applet can be selected on one logical channel
 * while the same applet instance or another applet instance from within the same package
 * is active on another logical channel.
 * <p>The methods of this interface are invoked by the JCRE only when :
 * <ul>
 * <li> the same applet instance is still active on another logical channel OR
 * <li> another applet instance from the same package is still active on another logical
 * channel.
 * </ul>
 * <p> See <em>Java Card Runtime Environment (JCRE) Specification</em> for details.
 */
public interface MultiSelectable {

  /**
   * Called by the JCRE to inform that this applet instance has been selected while
   * the same applet instance or another applet instance from the same package is
   * active on another logical channel
   * <p>It is called either when the MANAGE CHANNEL APDU (open)  command or
   * the SELECT APDU command is received and before the applet instance is selected.
   * SELECT APDU commands use instance AID bytes for applet
   * selection.
   * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section
   * 4.2 for details.<p>
   * A subclass of <code>Applet</code> should, within this method,
   * perform any initialization that may be required to
   * process APDU commands that may follow.
   * This method returns a boolean to indicate that it is ready to accept
   * incoming APDU
   * commands via its <code>process()</code> method. If this method returns
   * false, it indicates to the JCRE that this applet instance declines to be selected.
   * <p>Note:<ul>
   * <li><em>The <CODE>javacard.framework.Applet.select(</CODE>) method is not
   * called if this method is invoked.</em>
   * </ul>
   * @param appInstAlreadyActive boolean flag is <CODE>true</CODE> when the same applet
   * instance is already active on another logical channel and <CODE>false</CODE> otherwise
   * @return <CODE>true</CODE> if the applet instance accepts selection, <CODE>false</CODE> otherwise
   */
  public boolean select(boolean appInstAlreadyActive);

  /**
   * Called by the JCRE to inform that this currently selected applet instance is
   * being deselected on this logical channel while the
   * same applet instance or another applet instance from the same package is still active
   * on another logical channel. After deselection, this
   * logical channel will be closed or another applet instance
   * (or the same applet instance) will be selected on this logical channel.
   * It is called when a SELECT APDU command or a MANAGE CHANNEL (close)
   * command is received by the JCRE. This method is invoked
   * prior to another applet instance's or this very applet instance's <code>select()</code> method
   * being invoked.
   * <p>
   * A subclass of <code>Applet</code> should, within this method, perform
   * any cleanup or bookkeeping work before another applet instance is selected or the
   * logical channel is closed.
   * <p>Notes:<ul>
   * <li><em>The <CODE>javacard.framework.Applet.deselect(</CODE>) method is
   * not called if this method is invoked.</em>
   * <li><em>Unchecked exceptions thrown by this method are caught and ignored
   * by the JCRE but the applet instance is deselected.</em>
   * <li><em>The JCRE does NOT clear any transient objects of
   * </em><code>JCSystem.CLEAR_ON_DESELECT</code><em> clear event type owned
   * by this applet instance since at least one applet instance from the same
   * package is still active.</em>
   * <li><em>This method is NOT called on reset or power loss.</em>
   * </ul>
   * @param appInstStillActive boolean flag is <CODE>true</CODE> when the same applet instance
   * is still active on another logical channel and <CODE>false</CODE> otherwise
   */
  public void deselect(boolean appInstStillActive);
    
}

