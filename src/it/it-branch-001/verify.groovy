import org.apache.commons.lang.StringUtils

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

class Checker
{
  boolean result = true;

  File basedir;

  public Checker(File basedir) {
    this.basedir = basedir;
  }

  def readXPath( String pom, String xPathExpression )
  {
    def stream = new FileInputStream( new File( basedir, pom ) );
    try
    {
      return XPathFactory.newInstance()
              .newXPath()
              .evaluate( xPathExpression, DocumentBuilderFactory.newInstance()
              .newDocumentBuilder()
              .parse( stream ).documentElement );
    }
    finally
    {
      stream.close();
    }
  }

  Checker check( String message, String pom, String xpath, String expected )
  {
    if ( result )
    {
      try
      {
        def actual = readXPath( pom, xpath )
        if ( !StringUtils.equals( expected, actual ) )
        {
          System.out.println( pom + " [xpath:" + xpath + "] expected '" + expected + "' found '" + actual + "' : " + message );
          result = false;
        }
      }
      catch ( Throwable t )
      {
        t.printStackTrace();
        result = false;
      }
    }
    return this;
  }
}

return new Checker(basedir)
        .check( "branch root version", "pom.xml", "/project/version", "1.2.3-XOO-123-SNAPSHOT" )
        .check( "preserve top property top dependency", "pom.xml", "/project/properties/lib1.version", "1.0-SNAPSHOT" )
        .check( "branch top property top dependency", "pom.xml", "/project/properties/lib2.version", "2.0-XOO-123-SNAPSHOT" )
        .check( "preserve top property module dependency", "pom.xml", "/project/properties/lib3.version", "3.0-SNAPSHOT" )
        .check( "branch top property module dependency", "pom.xml", "/project/properties/lib4.version", "4.0-XOO-123-SNAPSHOT" )
        .check( "branch module version", "module/pom.xml", "/project/parent/version", "1.2.3-XOO-123-SNAPSHOT" )
        .result;
