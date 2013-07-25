#PDF Clown CJK Fonts

##License

[LGPL v3](http://www.gnu.org/licenses/lgpl.html)

##Synopsis

This package adds support for Chinese, Japanese, and Korean fonts in [PDFClown](http://www.stefanochizzolini.it/en/projects/clown/). It uses
non-embedded Type0 CID fonts so the client computer will need to have the font installed
to be able to see the text.  Typically, Adobe Reader will prompt the user to download the appropriate 
font-pack if they try to open a PDF for which they don't have fonts.

##Requirements

* [PDFClown](http://www.stefanochizzolini.it/en/projects/clown/).  Tested on 0.1.2 but should work on older versions also.
* Java 7 (It could probably be compiled for Java 6, but I might be using some Java 7 constructs, and Java 7 is all I need for now).


##Usage

Make sure that the PDFClownCIDCJKFont.jar has been added to your classpath.

    // Write some Japanese
    Font japaneseFont = CJKFont.loadJapanese(doc);
    composer.setFont(japaneseFont, 18);
    composer.showText("1234こんにちは世界", new Point2D.Double(10,10));

    // Write some Simplified Chinese
    Font simpChineseFont = CJKFont.loadChineseSimplified(doc);
    composer.setFont(simpChineseFont, 18);
    composer.showText("特色条目", new Point2D.Double(10, 40);


    // Write some Traditional Chinese
    Font tradChineseFont = CJKFont.loadChineseTraditional(doc);
    composer.setFont(tradChineseFont, 18);
    composer.showText("弱冠擢進士第", new Point2D.Double(10, 70);

    // Write some Korean
    Font koreanFont = CJKFont.loadKorean(doc);
    composer.setFont(koreanFont, 18);
    composer.showText("위키백과, 우리 모두의 백과사전", new Point2D.Double(10,100));

##Credits

* Used the fantastic [TCPDF](http://www.tcpdf.org/) PHP Library as a reference.
* Includes TCPDF's converted PHP files for the unicode mappings and font specifications.
* This library would have no reason to exist if groundwork had not already been laid by Stefano Chizzolini's superb [PDF Clown library](http://www.stefanochizzolini.it/en/projects/clown/).
* [iText® RUPS](https://sourceforge.net/projects/itextrups/) was an invaluable tool for being able to inspect the PDF files that were produced while trying to sort out the font encodings.


##Contact

[@shannah78](https://twitter.com/shannah78)