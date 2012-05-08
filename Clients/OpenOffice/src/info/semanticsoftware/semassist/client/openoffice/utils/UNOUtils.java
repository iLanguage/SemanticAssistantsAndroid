/*
Semantic Assistants -- http://www.semanticsoftware.info/semantic-assistants

This file is part of the Semantic Assistants architecture.

Copyright (C) 2009 Semantic Software Lab, http://www.semanticsoftware.info
Nikolaos Papadakis
Tom Gitzinger

The Semantic Assistants architecture is free software: you can
redistribute and/or modify it under the terms of the GNU Affero General
Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package info.semanticsoftware.semassist.client.openoffice.utils;

import java.io.*;

import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.NoSuchElementException;

import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XController;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.FrameSearchFlag;

import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XText;
import com.sun.star.text.XTextFieldsSupplier;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;

import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.WrappedTargetException;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextField;
import com.sun.star.util.XSearchDescriptor;
import com.sun.star.util.XReplaceable;
import com.sun.star.uri.ExternalUriReferenceTranslator;
import com.sun.star.uri.XExternalUriReferenceTranslator;

import info.semanticsoftware.semassist.csal.ClientUtils;
import info.semanticsoftware.semassist.csal.result.Annotation;
import info.semanticsoftware.semassist.csal.XMLElementModel;
import java.util.*;
import java.net.*;

public class UNOUtils
{
    private static final int HIGHLIGHT_YELLOW = 0x00FFFD00;
    private static final int HIGHLIGHT_OFF = 0xFFFFFF0A;
    private static int CURRENT_HIGHLIGHT = HIGHLIGHT_YELLOW;

    private static XMultiServiceFactory mxDocFactory = null;
    private static XTextCursor mxDocCursor = null;
    private static XSearchDescriptor mxSearchDescr = null;
   
    private static XReplaceable mxSearchable = null;  /* both for search & replace */
   
    private static XText mxAnnotText = null;
    private static String mCurrentPipeline;

    /** Signature to identify plug-in generated annotations. */
    private static final String APP = "SemanticAssistant";


    /**
     * Retrieves the URI of the current document.
     *
     * @param ctx CopenOffice context.
     * @return uri for the document if available, empty if document
     *             not saved or null on error.
     */
    public static URI getDocumentURI( final XComponentContext ctx )
    {
      URI result;
      try {
         final XModel model = getActiveDocumentModel(ctx);
         result = new URI(model.getURL());
      } catch (final URISyntaxException ex) {
         System.out.println("Error retrieving URI from document context.");
         result = null;
      }
      return result;
    }

    /**
     * Determines if a document is currently loaded.
     *
     * @param ctx OpenOffice context.
     * @param url String of the URL. Use empty string for documents that have
     *            not been saved.
     *
     * @return True if @a url is among the loaded documents, false otherwise.
     */
    public static boolean isDocumentLoaded(final XComponentContext ctx, final String url)
    {
      // Normalize URL argument to be an OpenOffice internal URL
      // representation. This differs in some cases from Java's URL
      // representation.
      final String normURL = translateURL(ctx, url, false);
      if (normURL == null) {
         System.err.println("Cannot normalize url <"+ url +">");
         return false;
      }

      final XEnumeration docs = getDesktop(ctx).getComponents().createEnumeration();
      while (docs.hasMoreElements()) {
         try {
            final XModel elem =
               UnoRuntime.queryInterface(XModel.class, docs.nextElement());

            if (normURL.equals(elem.getURL())) {
               return true;
            }               
         } catch (final com.sun.star.container.NoSuchElementException ex) {
            ex.printStackTrace(System.err);
         } catch (final com.sun.star.lang.WrappedTargetException ex) {
            ex.printStackTrace(System.err);
         } catch (final Exception ex) {
            System.err.println(ex.getMessage());
         }
      }
      return false;
   }

    /**
     * Retrieves either the marked text of the current document
     * or, if nothing is marked, the whole text of the document.
     */
    public static String getArgumentText( final XComponentContext ctx )
    {
        // Get the XModel interface from the active document,
        // and its controller
        XModel xModel = getActiveDocumentModel( ctx );
        XController xController = xModel.getCurrentController();

        // The controller gives us the TextViewCursor
        // Query the viewcursor supplier interface
        XTextViewCursorSupplier xViewCursorSupplier =
                                UnoRuntime.queryInterface(
                XTextViewCursorSupplier.class, xController );

        // Get the cursor
        XTextViewCursor xViewCursor = xViewCursorSupplier.getViewCursor();
        String result = xViewCursor.getString();

        if( result == null || result.equals( "" ) )
        {
            final XTextDocument xDoc = UnoRuntime.queryInterface( XTextDocument.class, xModel );
            XText wholeText = xDoc.getText();
            return wholeText.getString();
        }
        else
        {
            return result;
        }

    }


    /**
     * Spawns an OpenOffice window with a new document from a given text.
     *
     * @param ctx Context.
     * @param text Text content for the new document.
     *
     * @return True if operation was successful, false otherwise.
     */
    public static boolean createNewDoc( final XComponentContext ctx, final String text )
    {
        boolean status = true;
        try {
            // Get an new empty document.
            final XTextDocument doc = UnoRuntime.queryInterface(
               com.sun.star.text.XTextDocument.class,
               loadDoc(ctx, "private:factory/swriter"));

            // Add text content to document.
            doc.getText().setString(text);
        } catch( final Exception ex ) {
            System.err.println("Failed to create textual document");
            ex.printStackTrace( System.err );
            status = false;
        }
        return status;
    }


    /**
     * Spawns an OpenOffice window with a document from a given file.
     *
     * @param ctx Context.
     * @param file Local file from which to get content for new document.
     *
     * @return True if operation was successful, false otherwise.
     */
    public static boolean createNewDoc( final XComponentContext ctx, final File f )
    {
        return loadDoc(ctx, f.toURI().toString()) == null ? false : true;
    }

    /**
     * Helper method to spawn an OpenOffice window with a new or existing document.
     *
     * @param ctx Contex.
     * @param url URL of the existing document or protocol for the new.
     *
     * @return Component of the spawned frame if successful or null otherwise.
     */
    private static XComponent loadDoc(final XComponentContext ctx, final String url)
    {
      XComponent comp = null;

      // Normalize URL argument to be an OpenOffice internal URL
      // representation. This differs in some cases from Java's URL
      // representation.
      final String normURL = translateURL(ctx, url, false);
      if (normURL != null) {
         // Query the XComponentLoader interface from the Desktop service
         final XComponentLoader xComponentLoader = UnoRuntime.queryInterface(
            XComponentLoader.class, getDesktop(ctx) );

         try {
            // Retrieve any existing frames with the same URL, 
            // else create new ones.
            comp = xComponentLoader.loadComponentFromURL(
               normURL, "_default", FrameSearchFlag.CREATE, new PropertyValue[0]);

         } catch( final com.sun.star.io.IOException ex ) {
            System.err.println("Invalid URL <"+ url +">"); 
            ex.printStackTrace();
         } catch( final com.sun.star.lang.IllegalArgumentException ex ) {
            System.err.println("Invalid properties");
            ex.printStackTrace();
         }
      }
      return comp;
    }

    public static void createDocAnnotations( final XComponentContext ctx, final Annotation annotation )
    {
        // get the active document
        XTextDocument doc = getActiveTextDocument( ctx );

        mxDocFactory = UnoRuntime.queryInterface(
                XMultiServiceFactory.class, doc );

      // Search for the annotation in the document.
      final XTextRange range = findAnnotation(annotation);
      if (range == null) {
         System.err.println("Annotation not found in document.");
         return;
      }

      // Position cursor at annotation text occurance.
      mxAnnotText = range.getText();
      mxDocCursor = mxAnnotText.createTextCursorByRange(range);
      mxDocCursor.gotoRange(range, false);
      mxDocCursor.gotoRange(range, true);

      // TODO: Better encapsulate side-effect depenance on the above global
      // variables.
      annotateField(annotation);
    }

    /**
     * Helper method to represent paths as internal or external URLs.
     *
     * @param ctx Context
     * @param url URL to translate.
     * @param mode TRUE to translate @a url to an external URL.
     *             FALSE to translate @a url to an internal URL.
     *
     * @return The translated URL if successful or null otherwise.
     */
    private static final String translateURL( final XComponentContext ctx, final String url , final boolean mode ) {
      final XExternalUriReferenceTranslator translator = ExternalUriReferenceTranslator.create(ctx);

      final String normURL = mode ?
         translator.translateToExternal(url) : translator.translateToInternal(url);
      return (normURL.length() == 0 && url.length() != 0) ? null : normURL;
    }

    private static final void initializeCursor_internal( final XComponentContext ctx, final XComponent comp)
    {
      try {
         final XTextDocument doc = UnoRuntime.queryInterface( XTextDocument.class, comp);

         mxDocFactory = UnoRuntime.queryInterface(
            XMultiServiceFactory.class, doc);
         mxSearchable = UnoRuntime.queryInterface( XReplaceable.class, doc );

         mxSearchDescr = mxSearchable.createSearchDescriptor();
      } catch (final Exception ex) {
         ex.printStackTrace(System.err);
      }
    }

    /**
     * Assign cursor focus to either a saved or unsaved document.
     *
     * @param ctx Context
     * @param url Url of document being assigned focus. Accepts
     *            null or empty strings for yet unsaved documents.
     */
    public static void initializeCursor(final XComponentContext ctx, final String url) {
      // NOTE: This public method should be privatized & encapsulated 
      // to avoid race conditions where caller manually changes window
      // focus between between the time a loaded document is first given
      // focus up to UNOUtils.createDocAnnotations() is invoked.
      if (url != null && !"".equals(url)) {
         initializeCursor_internal(ctx, loadDoc(ctx, url));
      } else {
         initializeCursor_internal(ctx, getActiveTextDocument( ctx ));
      }
    }

   /**
    * Removes all annotations within the active document generated by
    * a given Semantic Assistant pipeline.
    *
    * @param ctx OpenOffice context.
    * @param pipeline Name of the pipeline that generated the annotations
    *                 to remove.
    */
   public static void clearDocAnnotations(final XComponentContext ctx, final String pipeline) {
      try {
         // Retrieve text of the current document.
         final XTextDocument doc = getActiveTextDocument(ctx);
         final XText txt = doc.getText();

         // Enumerate all text-fields within the text.
         final XTextFieldsSupplier supplier =
            UnoRuntime.queryInterface(XTextFieldsSupplier.class, doc);
         final XEnumerationAccess access = supplier.getTextFields();
         final XEnumeration enumeration = access.createEnumeration();

         // Iterate through each-text field searching for annotations.
         while (enumeration.hasMoreElements()) {
            // Contextualize element & determine if it is an Annotation.
            final Object elem = enumeration.nextElement();
            final XServiceInfo info =
               UnoRuntime.queryInterface(XServiceInfo.class, elem);

            // Only remove SemanticAssistant generated side-notes.
            if (info.supportsService("com.sun.star.text.TextField.Annotation")) {
               final XTextField field = UnoRuntime.queryInterface(XTextField.class, elem); 
               clearAnnotation(txt, field, pipeline);
            }
         }
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }
   public static void clearDocAnnotations(final XComponentContext ctx) {
      clearDocAnnotations(ctx, null);
   }

   /**
    * Removes all semantic assistant pipeline generated annotation from the text.
    *
    * @param txt Text object containing the field.
    * @param fld Annotation text field (side-note) to be removed.
    * @param pipeline Name of the pipeline that generated the annotation. Null
    *                 to remove annotations from any pipeline.
    *
    * @return The status of the operation.
    */
   private static boolean clearAnnotation(final XText txt, final XTextField fld, final String pipeline) {
      try {
         final XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, fld);
         final String author = props.getPropertyValue("Author").toString();
         final boolean del = (pipeline != null) ?
            author.equals(APP +":"+ pipeline) : author.startsWith(APP +":");

         // Remove side-note & highlighting.
         if (del) {
            //TODO: Need to get document location of field to construct
            // a cursor in order to remove highlighting. The following
            // does not quite do the trick.
            //final XTextRange range = fld.getAnchor();
            //highlightField(range.getText().createTextCursorByRange(range), false);
            txt.removeTextContent(fld);
         }
      } catch (final Exception ex) {
         ex.printStackTrace();
         return false;
      }
      return true;
   }

    /**
     * @return the mCurrentPipeline
     */
    public static String getCurrentPipeline()
    {
        return mCurrentPipeline;
    }

    /**
     * @param aMCurrentPipeline the mCurrentPipeline to set
     */
    public static void setCurrentPipeline( final String aMCurrentPipeline )
    {
        mCurrentPipeline = aMCurrentPipeline;
    }

    /**
     * Get the currently active text document
     */
    private static XTextDocument getActiveTextDocument( final XComponentContext ctx )
    {
        XModel xDocModel = getActiveDocumentModel( ctx );
        return UnoRuntime.queryInterface( XTextDocument.class, xDocModel );
    }

    private static XModel getActiveDocumentModel( final XComponentContext ctx )
    {
        XComponent document = getCurrentComponent( ctx );

        return UnoRuntime.queryInterface( XModel.class, document );
    }

    private static XComponent getCurrentComponent( final XComponentContext ctx )
    {
        XDesktop xDesktop = getDesktop( ctx );
        XComponent document = xDesktop.getCurrentComponent();

        return document;
    }

    /**
     * Get the desktop service
     */
    private static XDesktop getDesktop( final XComponentContext ctx )
    {
        XMultiComponentFactory xmcf = ctx.getServiceManager();
        Object desktop = null;
        try
        {
            desktop = xmcf.createInstanceWithContext( "com.sun.star.frame.Desktop", ctx );
        }
        catch( com.sun.star.uno.Exception e )
        {
        }
        return UnoRuntime.queryInterface( com.sun.star.frame.XDesktop.class, desktop );
    }

    private static void annotateField( final Annotation annotation )
    {
        try {
            final XTextField annot = makeAnnotation(annotation);
            mxAnnotText.insertTextContent(mxDocCursor, annot, false);
        } catch(Exception e) {
            e.printStackTrace();
        }

        // Highlight annotated field
        if (ClientPreferences.isTextHighlightMode()) {
            highlightField(mxDocCursor, true);
        }
    }

    /**
     * Highlights the region of a given cursor.
     *
     * @param cursor Region to highlight.
     * @param enable True to highlight, false to de-highlight.
     */
    private static boolean highlightField(final XTextCursor cursor, final boolean enable) {
      boolean status = true;
      try {
         final XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, cursor);
         props.setPropertyValue("CharBackColor", enable ? CURRENT_HIGHLIGHT : HIGHLIGHT_OFF);
      } catch (final Exception ex) {
         ex.printStackTrace();
         status = false;
      }
      return status;
    }

    /**
     * Create an annotation (side-note) textfield object.
     *
     * @param Annotation Memory representation of the annotation.
     *
     * @throws Exception
     */
    private static XTextField makeAnnotation(final Annotation annotation)
      throws Exception
    {
      // Create a side-note object & get its properties to be modified.
      final XTextField annot = UnoRuntime.queryInterface(
         XTextField.class, mxDocFactory.createInstance(
         "com.sun.star.text.TextField.Annotation"));
      final XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, annot);

      // Text is for embedding complex objects into side-notes.
      final XText text = UnoRuntime.queryInterface(
         XText.class, props.getPropertyValue("TextRange"));
      text.insertString(text, "type= "+ annotation.mType +"\n", false);

      // Configure look-&-feel of side-note information.
      setFontSize(text, ClientPreferences.getSideNoteFontSize());

      // If configured, duplicate annotation content as part of the side-note.
      if (ClientPreferences.isShowAnnotationContent()) {
         text.insertString(text, "content= "+ annotation.mContent +"\n", false);
      }

      // Iterate through annotation features.
      final Set<String> keys = annotation.mFeatures.keySet();
      final Iterator<String> iter = keys.iterator();

      while (iter.hasNext()) {
         final String key = iter.next();
         final String val = annotation.mFeatures.get(key);

         // If configured, suppress empty-valued features.
         if (ClientPreferences.isEmptyFeatureFilter() && "".equals(val)) {
            System.out.println("---------------- Ignoring empty valued feature: "+ key);
            continue;
         }

         // Make URL features hyper-linkable.
         if ("url".equalsIgnoreCase(key)) {
            text.insertString(text, key +"= ", false);
            text.insertTextContent(text, makeHyperLink(val, val), false);
            text.insertString(text, "\n", false);
         } else {
            text.insertString(text, key +"= "+ val +"\n", false);
         }
      }

      // Define side-note properties.
      try {
         // NOTE: It is not clear from OpenOffice's API if the "Content" property
         // is strictly required, which are the default values, nor what are the
         // consequences of (not) having it.
         //props.setPropertyValue("Content", annot.getPresentation(false));
         props.setPropertyValue("Author", APP +":"+ mCurrentPipeline);
      } catch (UnknownPropertyException e) {
         /* Thrown ONLY on programming/typo error of the try body! */
         e.printStackTrace();
      }

      return annot;
    }

    /**
     * Create a hyperlink textfield object.
     *
     * @param linkURL Address of the hyperlink including its protocol (ie: http://)
     * @param linkName Symbolic name of the hyperlink.
     *
     * @throws Exception
     */
    private static XTextField makeHyperLink(final String linkURL, final String linkName)
      throws Exception
    {
      //Create a URL object & gets its properties to be modified.
      final XTextField link = UnoRuntime.queryInterface(
         XTextField.class, mxDocFactory.createInstance(
         "com.sun.star.text.TextField.URL"));
      final XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, link);

      // Define URL properties.
      try {
         props.setPropertyValue("Representation", linkName);
         props.setPropertyValue("URL", linkURL);
      } catch (UnknownPropertyException e) {
         /* Thrown ONLY on programming/typo error of the try body! */
         e.printStackTrace();
      }

      return link;
    }

    /**
     * Configure the font-size of a range of text.
     *
     * @param range of text to change the font.
     * @param size value of the new font.
     */
    private static void setFontSize(final XTextRange range, final float size)
    {
        // Extract the cursor properties & change its font-size.
        final XTextCursor cursor = range.getText().createTextCursorByRange(range);
        final XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, cursor);
        try {
            props.setPropertyValue("CharHeight", size);
        } catch (UnknownPropertyException e) {
            e.printStackTrace();
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        } catch (com.sun.star.lang.WrappedTargetException e) {
            e.printStackTrace();
        } catch (com.sun.star.lang.IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private static boolean isTextAnnotated(final XTextCursor cursor) {
      boolean result = false;
      final XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, cursor);
      try {
         final Object property = props.getPropertyValue("CharBackColor");
         result = (property.equals( HIGHLIGHT_OFF ) || property.equals( HIGHLIGHT_YELLOW ));
      } catch (final Exception ex) {
          ex.printStackTrace();
      }
      return result;
    }

    /**
     * Searches the document for the occurrence of the annotation.
     *
     * @param annot Annotation to search in the document
     * @return TextRange of the occurring annotation in the document
     *         if it is found, null otherwise.
     *
     * @note The implemented annotation occurrence resolution strategy
     * simply relies on string matching. This will fail in cases when
     * a string pattern occurs multiple times in the document but not
     * all have the same annotation (if any).
     */
    private static XTextRange findAnnotation(final Annotation annot) {
      // Configure search settings.
      try {
         mxSearchDescr.setPropertyValue("SearchWords", Boolean.valueOf(true));
         mxSearchDescr.setPropertyValue("SearchCaseSensitive", Boolean.valueOf(true));
         mxSearchDescr.setSearchString(annot.mContent);
      } catch (final Exception ex) {
         // Interested in: UnknownProperty, WrappedTarget & IllegalArgument exceptions.
         System.err.println("Could not configure search.");
         ex.printStackTrace();
         return null;
      }

      // Do the search.
      Object search = null;
      try {
         search = mxSearchable.findFirst(mxSearchDescr);
      } catch (final com.sun.star.uno.RuntimeException ex) {
         System.out.println("No more annotations to search for.");
         return null;
      }

      /* NOTE: The following error-reduction strategy may not be needed
         nor suitable for all search usages.

      // Convert to proper type.
      final XTextRange result = UnoRuntime.queryInterface(XTextRange.class, search);
      if (result == null) {
         return null;
      }

      // Position cursor on annotation span.
      final XText annotTxt = result.getText();
      final XTextCursor cursor = annotTxt.createTextCursor();
      cursor.gotoRange(result, false);
      cursor.gotoRange(result, true);

      // Keep searching if any hits are not annotation instances.
      while (!isTextAnnotated(cursor)) {
         try {
            search = mxSearchable.findNext(search, mxSearchDescr);
         } catch (final com.sun.star.uno.RuntimeException ex) {
            System.out.println("No more annotations to search for.");
            break;
         }
      }
      */

      // Convert to proper type.
      return (search == null) ? null :
         UnoRuntime.queryInterface(XTextRange.class, search);
    }


    public static boolean replaceAnnotation(final Annotation annot, final String str) {
      // Search for the annotation.
      final XTextRange found = findAnnotation(annot);
      if (found == null) {
         System.err.println("Annotation not found in document.");
         return false;
      }

      // Position document cursor on searched annotation.
      final XText text = found.getText();
      final XTextCursor cursor = text.createTextCursor();
      cursor.gotoRange(found, false);
      cursor.gotoRange(found, true);

      // Replace its content.
      try {
         text.insertString(cursor, str, true);
      } catch (final Exception ex) {
         System.err.println("Could not replace annotation text in document.");
         return false;
      }
      return true;
    }

    // FIXME: Duplication from Eclipse Utils.java to be consolidated in CSAL
    // once all duplicated client ServiceAgentSingletons implementations are
    // refactored.
    public static void propertiesReader()
      throws NullPointerException {
		// Should return only one item in the list
	   ArrayList<XMLElementModel> server = ClientUtils.getClientPreference(ClientPreferences.CLIENT_NAME, "server");
		
   	// if there are no server defined for this client. then look for the last called one in the global scope
		if (server.size() == 0) {
	      server = ClientUtils.getClientPreference(ClientUtils.XML_CLIENT_GLOBAL, "lastCalledServer");
   	}
   	// Note that if the former case, if by mistake there are more than
      // one server defined, we pick the first one. If the specific host/port
      // attributes are not found, the preference file is corrupt &
      // implicitly throw an exception.
		ServiceAgentSingleton.setServerHost(server.get(0).getAttribute().get(ClientUtils.XML_HOST_KEY));
	   ServiceAgentSingleton.setServerPort(server.get(0).getAttribute().get(ClientUtils.XML_PORT_KEY));
    }

   /**
    * Helper method to troubleshoot OpenOffice object properties.
    *
    * @param props Property set being explored.
    *
    * @see http://wiki.services.openoffice.org/wiki/Documentation/DevGuide/ProUNO/Properties
    */
   private static final void printXPropertySet(final XPropertySet props) {
      try {
         // get the property info interface of this XPropertySet
         final com.sun.star.beans.XPropertySetInfo info = props.getPropertySetInfo();
 
         // get all properties (NOT the values) from XPropertySetInfo
         final com.sun.star.beans.Property[] p = info.getProperties();
         for (int i = 0; i < p.length; ++i) {
            System.out.print("Property #" + i);
            System.out.print(": Name<" + p[i].Name);
            System.out.print("> Handle<" + p[i].Handle);
            System.out.print("> " + p[i].Type.toString());
 
            // attributes (flags)
            System.out.print(" Attributes<");
            final short nAttribs = p[i].Attributes;
            if ((nAttribs & com.sun.star.beans.PropertyAttribute.MAYBEVOID) != 0)
                 System.out.print("MAYBEVOID|");
            if ((nAttribs & com.sun.star.beans.PropertyAttribute.BOUND) != 0)
                 System.out.print("BOUND|");
            if ((nAttribs & com.sun.star.beans.PropertyAttribute.CONSTRAINED) != 0)
                 System.out.print("CONSTRAINED|");
            if ((nAttribs & com.sun.star.beans.PropertyAttribute.READONLY) != 0)
                 System.out.print("READONLY|");
            if ((nAttribs & com.sun.star.beans.PropertyAttribute.TRANSIENT) != 0)
                 System.out.print("TRANSIENT|");
            if ((nAttribs & com.sun.star.beans.PropertyAttribute.MAYBEAMBIGUOUS ) != 0)
                 System.out.print("MAYBEAMBIGUOUS|");
            if ((nAttribs & com.sun.star.beans.PropertyAttribute.MAYBEDEFAULT) != 0)
                 System.out.print("MAYBEDEFAULT|");
            if ((nAttribs & com.sun.star.beans.PropertyAttribute.REMOVEABLE) != 0)
                 System.out.print("REMOVEABLE|");
            System.out.println("0>");
         }
     } catch (final Exception ex) {
         ex.printStackTrace(System.err);
     }
  }
}
