package www;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.filechooser.FileSystemView;

public final class 合并语言文件
{
	public static final void main( String[] args ) throws IOException
	{
		String source = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
		String path = source + File.separator + "cfgs"; // 默认是桌面上的 cfgs 文件夹
		String output = source + File.separator + "output.cfg"; // 默认是桌面上的 output.cfg 文件
		
		boolean isAlign = true;
		String alignPrefix = "";
		boolean showDuplicate = false;
		
		System.out.println( "操作开始" );
		
		File file = new File( path );
		if( ! file.exists() ) file.mkdirs();
		File[] files = file.listFiles();
		if( files.length < 1 )
		{
			System.out.println( "无法对空文件夹进行操作" );
			return ;
		}
		
		Map<String,Map<String,List<Data>>> data = new HashMap<String,Map<String,List<Data>>>();
		Map<String,List<Data>> commenPatch = new HashMap<String,List<Data>>();
		int count = 0;
		BufferedReader reader;
		String line;
		Map<String,List<Data>> patch = null;
		int pos;
		String key;
		String value;
		boolean isComment;
		List<Data> list;
		for( File sub : files )
		{
			if( sub.getName().endsWith( ".cfg" ) )
			{
				count ++;
				reader = new BufferedReader( new InputStreamReader( new FileInputStream( sub ) , "utf-8" ) );
				patch = null;
				while( ( line = reader.readLine() ) != null )
				{
					if( line.trim().length() < 1 ) continue;
					else if( line.startsWith( "[" ) && line.endsWith( "]" ) )
					{
						patch = data.get( line );
						if( patch == null )
						{
							patch = new HashMap<String,List<Data>>();
							data.put( line , patch );
						}
					}
					else
					{
						if( patch == null ) patch = commenPatch;
						
						pos = line.indexOf( "=" );
						if( pos < 0 )
						{
							if( line.startsWith( "#" ) || line.startsWith( ";" ) ) continue;
							continue;
							//多行翻译还没有处理
							//key = line.substring( 0 , pos );
							//value = line.substring( pos+1 );
						}
						else
						{
							key = line.substring( 0 , pos );
							value = line.substring( pos+1 );
						}
						if( key.startsWith( "#" ) || key.startsWith( ";" ) )
						{
							key = key.substring( 1 );
							isComment = true;
						}
						else isComment = false;
						list = patch.get( key );
						if( list == null )
						{
							list = new ArrayList<Data>();
							patch.put( key , list );
						}
						list.add( new Data( value , isComment ) );
					}
				}
				reader.close();
			}
		}
		
		int total = 0;
		for( String partKey : data.keySet() ) total += data.get( partKey ).size();
		
		System.out.println( "共检测到 "+files.length+" 个文件 , 处理了 "+count+" 个文件" );
		System.out.println( "共发现 "+total+" 个翻译条目（不算重复项目）" );
		System.out.println( "开始生成统一文件" );
		
		File out = new File( output );
		File parent = out.getParentFile();
		if( ! parent.exists() ) parent.mkdirs();
		if( ! out.exists() ) out.createNewFile();
		
		BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( output ) , "utf-8" ) );
		List<String> duplicateList = new ArrayList<String>();
		int lineCount = 0;
		lineCount += 合并语言文件.output( writer , commenPatch , isAlign , alignPrefix , "[空]" , duplicateList );
		for( String partKey : data.keySet() )
		{
			lineCount ++;
			writer.write( partKey+"\n" );
			writer.flush();
			
			patch = data.get( partKey );
			lineCount += 合并语言文件.output( writer , patch , isAlign , alignPrefix , partKey , duplicateList );
		}
		writer.close();
		System.out.println( "统一文件生成完毕 , 共 "+lineCount+" 行" );
		
		if( showDuplicate )
		{
			System.out.println( "输出重复项目 :" );
			for( String item : duplicateList ) System.out.println( "翻译项目重复 : "+item );
		}
		System.out.println( "共有重复翻译项目 "+duplicateList.size()+" 条" );
		
		System.out.println( "操作完毕" );
	}
	
	private static final int output( BufferedWriter writer , Map<String,List<Data>> patch , boolean isAlign , String alignPrefix , String duplicatePrefix , List<String> duplicateList ) throws IOException
	{
		int lineCount = 0;
		List<Data> list;
		for( String itemKey : patch.keySet() )
		{
			list = patch.get( itemKey );
			if( list.size() > 1 ) duplicateList.add( duplicatePrefix+"."+itemKey );
			if( isAlign )
			{
				while( itemKey.startsWith( " " ) || itemKey.startsWith( "\t" ) ) itemKey = itemKey.substring( 1 );
				itemKey = alignPrefix + itemKey;
			}
			for( Data item : list )
			{
				lineCount ++;
				if( item.isComment() ) writer.write( "#"+itemKey+"="+item.value()+"\n" );
				else writer.write( itemKey+"="+item.value()+"\n" );
				writer.flush();
			}
		}
		return lineCount;
	}
	
	private static final class Data
	{
		private String value;
		private boolean isComment;
		
		public Data( String value , boolean isComment )
		{
			this.value = value;
			this.isComment = isComment;
		}
		
		public final String value()
		{
			return this.value;
		}
		
		public final boolean isComment()
		{
			return this.isComment;
		}
	}
}