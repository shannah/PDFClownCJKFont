/*******************************************************************
PDFClown CJKFonts
Copyright (c) 2013 Web Lite Solutions Corp.
All rights reserved.

* 
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*********************************************************************/
package ca.weblite.pdfclown.documents.contents.fonts;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pdfclown.documents.Document;
import org.pdfclown.documents.contents.fonts.Font;
import org.pdfclown.objects.PdfArray;
import org.pdfclown.objects.PdfDictionary;
import org.pdfclown.objects.PdfInteger;
import org.pdfclown.objects.PdfName;
import org.pdfclown.objects.PdfReal;
import org.pdfclown.objects.PdfReference;
import org.pdfclown.objects.PdfString;
import org.pdfclown.objects.Rectangle;
import org.pdfclown.util.BiMap;
import org.pdfclown.util.ByteArray;
import org.pdfclown.util.ConvertUtils;

/**
 * <p>A Font for PDFClown that supports Chinese (Simplified & Traditional), Japanese,
 * and Korean characters.  It uses TCPDF's (http://www.tcpdf.org/) CID0 unicode fonts
 * for the font information (e.g. character widths).  It actually parses the PHP files
 * directly to get this information.</p>
 * <p>This could probably be adapted to parse font files directly but I found the 
 * PHP files more accessible at this point.</p>
 * 
 * <p>All of these languages specify that the ArialUnicodeMS font should be used
 * for rendering the Unicode text.  This requires that your PDF reader has this font
 * installed.  Typically, Adobe Reader will pop up with a message saying that some
 * fonts require the CJK language pack in order to work properly, then it will just download
 * the font kit and allow you to load the document.</p>
 * 
 * 
 * @author shannah
 */
public class CJKFont extends Font {

    
    /**
     * Flag to indicate whether the font has been loaded yet.
     */
    private boolean loaded = false;
    
    /**
     * The type of font.  E.g. cidfont0  (in fact always cidfont0)
     */
    private String type;
    
    /**
     * The name of the font.  Always "ArialUnicodeMS"
     */
    private String name;
    
    /**
     * $up Value from the font file.  This isn't currently used, but it is
     * in the TCPDF font file so we'll store it.
     */
    private double up;
    
    /**
     * $ut Value from the font file.  This isn't currently used, but it is
     * in the TCPDF font file so we'll store it.
     */
    private double ut;
    
    /**
     * Default width of the font as parsed from the PHP file.
     */
    private double dw;
    
    /**
     * Diff list of the font as parsed from the PHP file..  Always empty string.
     */
    private String diff;
    
    /**
     * Original size in bytes of the font file, as parsed from the PHP file.  Not used.
     */
    private long originalSize;
    
    /**
     * Encoding of the font.  This is used in the /BaseFont and /Encoding attributes
     * of the PDF font object.  It is parsed from the PHP file and will vary from font to font.
     */
    private String enc;
    
    /**
     * CidInfo map parsed from the PHP file.
     */
    private Map cidinfo;
    
    /**
     * Font descriptor map parsed from the PHP file.
     */
    private Map desc;
    
    /**
     * Maps unicode ID to character width.  Parsed from font file.
     */
    private Map<Integer,Integer> cw;
    
    /**
     * Path to the PHP file.
     */
    private String path;
    
    /**
     * The FontDescriptor that is returned by the getDescriptor() method.
     */
    private PdfDictionary descriptor;
    
    /**
     * The document to which this font has been added.
     */
    private Document document;
    
    /**
     * An inverse of the CMap that maps unicode IDs to its byte array equivalent.
     */
    private Map<Integer,ByteArray> cmap2;
    
    /**
     * The CID0Font object.
     */
    private PdfDictionary cid0Font;
    
    /**
     * The unicode to cid map.  This doesn't seem to be of much use right now.  Ultimately
     * this data is copied into the glyphIndexes map, so this map is actually redundant and
     * should be removed to preserve memory.  In this case, it seems that the CIDs don't have
     * a meaning since ArialUnicode seems to just use the Unicode ID as the glyph ID.
     */
    private Map<Integer,Integer> uni2cid;
    
    /**
     * Alternate input for the font using an input stream.
     */
    private InputStream fontInputStream;
    
    /**
     * Loads a TCPDFFont by providing the PHP file's path.
     * @param context The document to add the font to.
     * @param path The path to the font's PHP file.  I.e. a file inside the "fonts" 
     * directory of the TCPDF distribution.
     * @return A font that can be used to render text in a document.
     * @see loadChineseSimplified()
     * @see loadJapanese()
     * @see loadKorean()
     * @see loadChineseTraditional()
     */
    public static CJKFont get(Document context, String path){
        CJKFont f =  new CJKFont(context);
        f.path = path;
        f.load();
        return f;
    }
    
    /**
     * Loads a TCPDFFont by providing an input stream to the PHP font file.  If 
     * you use this method, you need to make sure that the supporting files
     * are in the classpath in the same package as the TCPDFFont class, because 
     * when the parser encounters an include() statement it will try to load the 
     * file using Class.getResourceAsStream()
     * 
     * 
     * @param context  The PDF file to which the font is to be added.
     * @param is The input stream with the PHP font file.
     * @return The font.
     * @see loadChineseSimplified()
     * @see loadJapanese()
     * @see loadKorean()
     * @see loadChineseTraditional()
     */
    protected static CJKFont get(Document context, InputStream is){
        CJKFont f =  new CJKFont(context);
        f.fontInputStream = is;
        f.load();
        return f;
    }
    
    /**
     * Loads a font that can be used to render chinese simplified.  The Font is
     * ArialUnicodeMS
     * @param context
     * @return A Chinese simplified Unicode font.
     */
    public static CJKFont loadChineseSimplified(Document context){
        return get(context, CJKFont.class.getResourceAsStream("cid0cs.php"));
    }
    
    /**
     * Loads a Chinese traditional font.  This uses the ArialUnicodeMS font as a base.
     * @param context
     * @return A Chinese traditional Unicode font.
     */
    public static CJKFont loadChineseTraditional(Document context){
        return get(context, CJKFont.class.getResourceAsStream("cid0ct.php"));
    }
    
    /**
     * Loads a Japanese font.  This uses the ArialUnicodeMS font as a base.
     * @param context The PDF Document to which the font is added.
     * @return A Japanese font.
     */
    public static CJKFont loadJapanese(Document context){
        return get(context, CJKFont.class.getResourceAsStream("cid0jp.php"));
    }
    
    /**
     * Loads a Korean font.  This uses the ArialUnicodeMS font as a base.
     * 
     * @param context
     * @return A Korean font.
     */
    public static CJKFont loadKorean(Document context){
        return get(context, CJKFont.class.getResourceAsStream("cid0kr.php"));
    }
    
    public static CJKFont loadArialUniCid0(Document context){
        return get(context, CJKFont.class.getResourceAsStream("arialunicid0.php"));
    }
    
    protected CJKFont(Document context){
        super(context);
        document = context;
        
        
    }

    @Override
    protected PdfDictionary getDescriptor() {
        if ( descriptor == null ){
            descriptor = new PdfDictionary();
        }
        return descriptor;
    }

    @Override
    protected void onLoad() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Loads the font from either a file located at this.path, or an InputStream
     * referenced in this.fontInputStream.  This will parse the PHP file for 
     * the font information and build the appropriate PDF data structure.
     * 
     * <p>Note that you must assign one of this.path or this.fontInputStream for 
     * this to work.  fontInputStream takes precendence if both are non-null.
     */
    @Override
    protected void load(){
        if ( loaded ){
            return;
        }
        loaded = true;
        this.symbolic = false;
        
        
        // Start out by populating the base font object with the main information 
        // about the font.
        this.getBaseDataObject().put(PdfName.Type, PdfName.Font);
        this.getBaseDataObject().put(PdfName.Subtype, PdfName.Type0);
        
        // Right now everything is based on ArialUnicodeMS.  We might change this
        // if more fonts are added later.
        this.getBaseDataObject().put(PdfName.BaseFont, new PdfName("ArialUnicodeMS"));
        
        // We need to create a CID0Font a sa descendant of the base font tag.
        cid0Font = new PdfDictionary();
        cid0Font.put(PdfName.Type, PdfName.Font);
        cid0Font.put(PdfName.Subtype,  PdfName.CIDFontType0);
        cid0Font.put(PdfName.BaseFont, new PdfName("ArialUnicodeMS"));
        
        // Register the CID0 font with the document and add it as an indirect object
        // to the base font as its descendant.
        PdfReference cid0Ref = document.getFile().register(cid0Font);
        this.getBaseDataObject().put(PdfName.DescendantFonts, new PdfArray(cid0Ref));
        BufferedReader r = null;
        try {
            
            // If no input stream was supplied for the font, we load the font
            // from its path.
            if ( fontInputStream == null && path != null ){
                 r = new BufferedReader(new FileReader(path));
            } else if ( fontInputStream != null ){
                // IF the font file was provided via an input stream use
                // that stream here.
                r = new BufferedReader(new InputStreamReader(fontInputStream));
            } else {
                
                // We use a runtime exception here because we don't want to declare
                // an exception in the method signature.. for no good reason.
                throw new RuntimeException("No path or input stream specified for the font.");
            }
            
            // Container to hold parsed line of the PHP file
            String line = null;
            
            // Scan the PHP file line by line and parse
            // contents.
            while ( (line = r.readLine()) != null ){
                
                if ( line.startsWith("include(")){
                    // This is the include line which likely includes the cid2unicode map
                    // check which unicode mapping it is.
                    String file = parseInclude(line);
                    BufferedReader r2 = null;
                    if ( fontInputStream == null ){
                        File parentDir = new File(path).getParentFile();
                        File inclFile = new File(parentDir, file);
                        r2 = new BufferedReader(new FileReader(inclFile));
                    } else {
                        r2 = new BufferedReader(new InputStreamReader(CJKFont.class.getResourceAsStream(file)));
                    }
                    
                    // Parse the supporting file.  This will be the file that contains
                    // the unicode to cid map.  (Although it seems that ArialUnicode 
                    // just uses the Unicode ID for the character ID so I don't know
                    // what this is even for!!!!
                    String line2 = null;
                    while ( (line2 = r2.readLine()) != null ){
                        String[] pieces2 = parseLine(line2);
                        if ( pieces2 == null ){
                            continue;
                        }
                        // The PHP file (e.g. uni2cid_ac15.php) includes
                        // only one useful line, that is the $cidinfo['uni2cid']= ...etc..
                        if ( "$cidinfo['uni2cid']".equals(pieces2[0])){
                            uni2cid = new HashMap<>();
                            cmap2 = new HashMap<>();
                            cidinfo.put("uni2cid", uni2cid);
                            parseIntMap(pieces2[1], uni2cid);
                            
                            glyphIndexes = new HashMap<>();
                            
                            // We need to implement our own getKey() method for
                            // biMap that just returns the value itself.  This is 
                            // because the uni2cid map is not a bijection.
                            codes = new BiMap<ByteArray,Integer>(){

                                @Override
                                public ByteArray getKey(Integer value) {
                                    return cmap2.get(value);
                                }

                                
                                
                            };
                            for(Map.Entry<Integer,Integer> cmapEntry : uni2cid.entrySet()){
                                ByteArray key = new ByteArray(ConvertUtils.numberToByteArray(cmapEntry.getKey(), 2, ByteOrder.BIG_ENDIAN)); // cid
                                int value = cmapEntry.getKey(); // unicode value
                                codes.put(
                                        key,
                                        value);
                                
                                // Since uni2cid is not a bijection we use a separate map here
                                // for the inverse relation.  This really needs to be cleaned
                                // up since it appears that we don't need to do any mapping at all
                                //  I.e. we might be able to completely get rid of the cmpa2 
                                // map.
                                cmap2.put(value, key);
                                
                            }
                            //glyphIndexes.putAll(uni2cid);
                            for ( int uniid : uni2cid.keySet() ){
                                
                                // Since ArialUnicode seems to just use Unicode 
                                // directly for its glyph IDs, we'll just turn
                                // the glyphIndexes map into an identify map.
                                // NOTE:  We are having trouble with widths not
                                // being loaded correctly, but this solution
                                // seems like the best of a batch of bad solutions.
                                // Have also tried using the uni2cid mapping but this 
                                // produces worse results
                                glyphIndexes.put(uniid, uniid);
                            }
                            
                            
                            
                        }
                    }
                    r2.close();
                }
                
                String[] pieces = parseLine(line);
                if ( pieces == null ){
                    continue;
                }
                switch (pieces[0]) {
                    case "$type":
                        type = pieces[1];
                        break;
                    case "$name":
                        name = pieces[1];
                        //getBaseDataObject().put(PdfName.Name, new PdfName(name));
                        break;
                    case "$up":
                        up = Double.parseDouble(pieces[1]);
                        break;
                    case "$ut":
                        ut = Double.parseDouble(pieces[1]);
                        break;
                    case "$dw":
                        dw = Double.parseDouble(pieces[1]);
                        defaultGlyphWidth = (int) dw;
                        
                        cid0Font.put(new PdfName("DW"), new PdfInteger((int)dw));
                        
                        break;
                    case "$diff":
                        diff = pieces[1];
                        break;
                    
                    case "$originalsize":
                        originalSize = Long.parseLong(pieces[1]);
                        break;
                    case "$enc":
                        enc = pieces[1];
                        getBaseDataObject().put(PdfName.Encoding, new PdfName(enc));
                        getBaseDataObject().put(PdfName.BaseFont, new PdfName("ArialUnicodeMS-"+enc));
                        break;
                    case "$cidinfo":
                        cidinfo = new HashMap();
                        parseArray(pieces[1], cidinfo);
                        
                        PdfDictionary dict = new PdfDictionary();
                        for ( Object k : cidinfo.keySet()){
                            Object v = cidinfo.get(k);
                            if ( v instanceof Integer ){
                                dict.put(new PdfName((String) k), new PdfInteger((int)v));
                            } else if ( v instanceof Double){
                                dict.put(new PdfName((String)k), new PdfReal((double)v));
                            } else {
                                dict.put(new PdfName((String)k), new PdfString((String)v));
                            }
                        }
                        cid0Font.put(PdfName.CIDSystemInfo, dict);
                        dict.put(PdfName.Ordering, new PdfString((String)cidinfo.get("Ordering")));
                        dict.put(PdfName.Registry, new PdfString((String)cidinfo.get("Registry")));
                        break;
                        
                    case "$desc":
                        desc = new HashMap();
                        parseArray(pieces[1], desc);
                        
                        
                        descriptor = getDescriptor();
                        descriptor.put(PdfName.Type, PdfName.FontDescriptor);
                        PdfInteger flags = new PdfInteger((int)desc.get("Flags"));
                        

                        descriptor.put(PdfName.Flags, flags);
                       
                        
                        int[] bbox = parseBBox((String)desc.get("FontBBox"));
                        descriptor.put(

                            PdfName.FontBBox,
                            new Rectangle(
                              new Point2D.Double(bbox[0], bbox[1]),
                              new Point2D.Double(bbox[2], bbox[3])
                              ).getBaseDataObject()
                            );
                        
                        Object[] todo = new Object[]{
                            PdfName.ItalicAngle, "ItalicAngle",
                            PdfName.Ascent, "Ascent",
                            PdfName.Descent, "Descent",
                            PdfName.Leading, "Leading",
                            PdfName.CapHeight, "CapHeight",
                            new PdfName("XHeight"), "XHeight",
                            PdfName.StemV, "StemV",
                            new PdfName("StemH"), "StemH",
                            new PdfName("AvgWidth"), "AvgWidth",
                            new PdfName("MaxWidth"), "MaxWidth",
                            new PdfName("MissingWidth"), "MissingWidth"
                        };
                        
                        
                        
                        for ( int i=0; i<todo.length; i+=2 ){
                            if ( desc.containsKey((String)todo[i+1])){
                                descriptor.put(
                                        (PdfName)todo[i], 
                                        new PdfInteger((int)desc.get((String)todo[i+1])));
                            }
                        }
                        
                        descriptor.put(PdfName.FontName, new PdfName("ArialUnicodeMS"));
                        
                        PdfReference ref = document.getFile().register(descriptor);
                        cid0Font.put(PdfName.FontDescriptor, ref);
                        
                        break;
                        
                    case "$cw":
                        // The character widths array
                        // The PHP file contains this array that seems to map
                        // unicode codes to the corresponding character width.
                        cw = new HashMap<>();
                        parseIntMap(pieces[1], cw);
                        
                        glyphWidths = new HashMap<>(cw);
                        Map<Integer,Integer> cid2Widths = new HashMap<>();
                        for ( Map.Entry<Integer,Integer> e : cw.entrySet()){
                            
                            int uni = e.getKey();
                            if ( uni == 0 ){
                                continue;
                            }
                            int w = e.getValue();
                            int cid = uni;
                            
                            if ( uni2cid.containsKey(uni)){
                                cid = uni2cid.get(uni);
                            } else if ( cid < 256 ){
                                
                            } else {
                                continue;
                            }
                            
                            cid2Widths.put(cid, w);
                        }
                        
                        List<Integer> cids = new ArrayList<>(cid2Widths.size());
                        cids.addAll(cid2Widths.keySet());
                        Collections.sort(cids, new Comparator<Integer>(){

                            @Override
                            public int compare(Integer o1, Integer o2) {
                                if ( o1.intValue() == o2.intValue() ) return 0;
                                if ( o1.intValue() < o2.intValue() ) return -1;
                                return 1;
                            }
                            
                        });
                        
                        // Now build the W array
                        PdfArray arr = new PdfArray();
                        
                        int currWidth = -1;
                        int startCid = -1;
                        int endCid = -1;
                        for ( int cid : cids){
                            
                            int thisWidth = defaultGlyphWidth;
                            try {
                                thisWidth = cid2Widths.get(cid);
                                
                            } catch ( Exception ex){
                                ex.printStackTrace();
                                continue;
                            }
                            if ( thisWidth == defaultGlyphWidth ){
                                if ( currWidth >=0 ){
                                    arr.add(new PdfInteger(startCid));
                                    arr.add(new PdfInteger(endCid));
                                    arr.add(new PdfInteger(currWidth));
                                    startCid = -1;
                                    endCid = -1;
                                    currWidth = -1;
                                    
                                } 
                                
                                continue;
                            }
                            if ( thisWidth != currWidth || cid > endCid+1  ){
                                if ( currWidth >=0 ){
                                    arr.add(new PdfInteger(startCid));
                                    arr.add(new PdfInteger(endCid));
                                    arr.add(new PdfInteger(currWidth));
                                    
                                } 
                                startCid = cid;
                                endCid = cid;
                                currWidth = thisWidth;
                            } else {
                                endCid = cid;
                            }
                            
                        }
                        if ( startCid >=0 ){
                            arr.add(new PdfInteger(startCid));
                            arr.add(new PdfInteger(endCid));
                            arr.add(new PdfInteger(currWidth));
                        }
                        
                        cid0Font.put(new PdfName("W"), arr);
                        
                        break;
                        
                }
            }
            
            r.close();
            super.load();
        } catch ( IOException ex){
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Parses a line in the PHP font file.  It will return a 2-element array
     * where the first element is the name of the variable that is being assigned
     * in this line, and the second is the value that is being assigned.  If the 
     * value is a string, it will return contents inside the string and won't include
     * the wrapping quotes.  If it is a number, it will return the number as is.
     * @param line
     * @return 
     */
    private String[] parseLine(String line){
        
        String[] out = new String[2];
        if ( !line.contains("=")){
            return null;
        }
        out[0] = line.substring(0, line.indexOf("=")).trim();
        String val = line.substring(line.indexOf("=")+1);
        if ( val.charAt(0) == '\''){
            int start = val.indexOf("'")+1;
            int end = val.lastIndexOf("\'");
            out[1] = val.substring(start, end).trim();
        } else {
            out[1] = val.substring(0, val.indexOf(";")).trim();
        } 
        
        return out;
    }
    
    /**
     * Parses a PHP array into a Map.
     * @param arr The string representation of a PHP array.
     * @param toMap A map to be filled with the array's contents.
     */
    private void parseArray(String arr, Map toMap){
        String[] vals = arr.substring("array(".length(), arr.lastIndexOf(")"))
                .replace("=>", ",")
                .split(",");
        for ( int i=0; i<vals.length; i+=2){
            String key = vals[i].trim();
            if ( key.startsWith("'") ){
                key = key.substring(1, key.lastIndexOf("'"));
            }
            String strVal = vals[i+1].trim();
            if ( strVal.startsWith("'")){
                toMap.put(key, strVal.substring(1, strVal.lastIndexOf("'")));
                continue;
            } else {
                try {
                    int val = Integer.parseInt(strVal);
                    toMap.put(key, val);
                    continue;
                } catch (NumberFormatException ex){}

                try {
                    double val = Double.parseDouble(strVal);
                    toMap.put(key, val);
                    continue;
                } catch ( NumberFormatException ex){}

            }
        }
    }
    
    /**
     * Parses a string containing a PHP associative array that maps ints to ints.
     * This will fill the provided Int->Int map with the equivalent data.
     * @param arrStr  String representation of a PHP array that maps ints to ints.
     * @param map The map to be filed with the parsed data.
     */
    private void parseIntMap(String arrStr, Map<Integer,Integer> map){
        String[] intStrings = arrStr
                .substring("array(".length(), arrStr.lastIndexOf(")"))
                .replace("=>",",")
                .split(",");
        int len = intStrings.length;
        for ( int i=0; i<len; i+=2){
            map.put(Integer.parseInt(intStrings[i]), Integer.parseInt(intStrings[i+1]));
        }
        
    }
    
    /**
     * Parses an PHP include statement and retrieves only the last file name
     * from the path inside it.  This depends on the include statement using 
     * single quotes to surround the file names.
     * @param line
     * @return The name of the file being included without its path.
     */
    private String parseInclude(String line){
        return line.substring(line.lastIndexOf("/")+1, line.lastIndexOf("'"));
    }
    
    /**
     * Parses a BBox represented as a String "[x y w h]" and returns
     * 4-element int array.
     * @param bbox String representation of a BBOX: "[x y w h]"
     * @return 4-element int array with the elements of the bbox.
     */
    private int[] parseBBox(String bbox){
        String[] entries = bbox.substring(bbox.indexOf("[")+1, bbox.lastIndexOf("]"))
                .split(" ");
        int[] out = new int[4];
        for (int i=0; i<4; i++){
            out[i] = Integer.parseInt(entries[i]);
        }
        return out;
        
    }
    
    
}
