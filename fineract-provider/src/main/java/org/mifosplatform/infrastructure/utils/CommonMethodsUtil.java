package org.mifosplatform.infrastructure.utils;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CommonMethodsUtil {
	 // check null object, String , Collections
   public static boolean isBlank(Object input){
          if(input == null){
               return true;
          }else if(input instanceof String){
               return (((String)input).trim().length()==0);
          }else if(input instanceof Collection<?>){
               return (((Collection<?>)input).size() == 0);
          }else if (input instanceof Map<?,?>){
              return (((Map<?, ?>)input).size() == 0);
          }else{
               return false;
          }
       }
   
		 // check if not null object, String , Collections
	   public static boolean isNotBlank(Object input){
		   return !(isBlank(input));
	       }
	   
   public static boolean isNotNull(Object input){
       if(input != null)
            return true;
       else
            return false;
    }
   public static boolean isNull(Object input){
       if(input == null)
            return true;
       else
            return false;
    }
   public static boolean isEmpty(Object input){
        if(input instanceof String){
            return (((String)input).trim().length()==0);
       }else if(input instanceof Collection<?>){
            return (((Collection<?>)input).size() == 0);
       }else if (input instanceof Map<?,?>){
           return (((Map<?, ?>)input).size() == 0);
       }else{
            return false;
       }
    }
   public static boolean isOne(int input){
       if(input == 1)
            return true;
       else
            return false;
    }
   public static boolean isZero(int input){
       if(input == 0)
            return true;
       else
            return false;
    }
 
	public static boolean isGreaterThanZero(int size) {
		if(size>0)
           return true;
      else
           return false;
	}
	public static boolean isLesserThanZero(int size) {
		if(size<0)
           return true;
      else
           return false;
	}
	public static boolean isLesserThanZero(long size) {
		if(size<0)
           return true;
      else
           return false;
	}
	public static boolean validateEmail(String email) { 
		boolean status=false; String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"; 
		Pattern pattern = Pattern.compile(EMAIL_PATTERN); 
		Matcher matcher=pattern.matcher(email); 
		if(matcher.matches()) 
			status=true;  
	 return status; 
	 }
}
