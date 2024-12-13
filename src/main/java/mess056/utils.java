/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056;

import static arcadeflex036.libc_old.*;

public class utils
{
	
	public static String strncpyz(String dest, String source, int len)
	{
		String s;
                char[] _dest = dest.toCharArray();
                
		if (len != 0) {
			strncpy(_dest, source, len - 1);
                        s = String.valueOf(_dest);
			_dest[len-1] = '\0';
                        dest = String.valueOf(_dest);
		}
		else {
			s = dest;
		}
		return s;
	}
	
	public static String strncatz(String dest, String source, int len)
	{
		int l;
		l = dest.length();
		dest += l;
		if (len > l)
			len -= l;
		else
			len = 0;
		return strncpyz(dest, source, len);
	}
	
/*TODO*///	void rtrim(char *buf)
/*TODO*///	{
/*TODO*///		size_t buflen;
/*TODO*///		char *s;
/*TODO*///	
/*TODO*///		buflen = strlen(buf);
/*TODO*///		if (buflen) {
/*TODO*///			for (s = &buf[buflen-1]; (*s == ' '); s--)
/*TODO*///				*s = '\0';
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	#ifndef strcmpi
/*TODO*///	int strcmpi(const char *dst, const char *src)
/*TODO*///	{
/*TODO*///		int result = 0;
/*TODO*///	
/*TODO*///		while( !result && *src && *dst )
/*TODO*///		{
/*TODO*///			result = tolower(*dst) - tolower(*src);
/*TODO*///			src++;
/*TODO*///			dst++;
/*TODO*///		}
/*TODO*///		return result;
/*TODO*///	}
/*TODO*///	#endif /* strcmpi */
/*TODO*///	
/*TODO*///	
/*TODO*///	#ifndef strncmpi
/*TODO*///	int strncmpi(const char *dst, const char *src, size_t n)
/*TODO*///	{
/*TODO*///		int result = 0;
/*TODO*///	
/*TODO*///		while( !result && *src && *dst && n)
/*TODO*///		{
/*TODO*///			result = tolower(*dst) - tolower(*src);
/*TODO*///			src++;
/*TODO*///			dst++;
/*TODO*///			n--;
/*TODO*///		}
/*TODO*///		return result;
/*TODO*///	}
/*TODO*///	#endif /* strncmpi */
	
}
