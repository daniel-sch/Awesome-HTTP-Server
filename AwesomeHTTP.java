import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AwesomeHTTP extends Thread {

	public static void main(String[] args) {
		int port = 0;
		if(args.length != 1){
			System.out.println("Usage: java AwesomeHTTP portnumber");
			System.exit(1);
		}
		try {
			port = Integer.parseInt(args[0]);
		} catch(NumberFormatException e) {
		}
		if(port <=0 || port >= 65536) {
			System.out.println("That is not a valid portnumber");
			System.exit(2);
		}
		
		try {
			ServerSocket s = new ServerSocket(port);
			while (true) {
				Socket c = s.accept();
				AwesomeHTTP t = new AwesomeHTTP(c);
				t.start();
			}
		} catch (IOException e) {
			System.out.println("Network error:\n");
			e.printStackTrace();
		}
	}

	private Socket socket;

	public AwesomeHTTP(Socket s) {
		super(s.getInetAddress().toString());
		this.socket = s;
	}

	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

			StringBuilder requestString = new StringBuilder();
			String line = in.readLine();
			while (line != null && !line.isEmpty()) {
				requestString.append(line);
				requestString.append('\n');
				line = in.readLine();
			}

			if (line != null) {
				try {
					HttpRequest request = new HttpRequest(requestString.toString());
					handleRequest(request).writeOn(out);
					out.flush();
				} catch (HttpRequest.InvalidHttpRequest e) {
					new HttpResponse(400).writeOn(out);
				}
			}

			out.close();
			in.close();
			socket.close();
		} catch (IOException e) {
			System.out.println("Netzwerkfehler ist aufgetreten:");
			e.printStackTrace();
		}
	}

	private HttpResponse handleRequest(HttpRequest request) {
		if (!request.getMethod().equals("GET")) {
			HttpResponse response = new HttpResponse(405);
			response.addHeader("Allow", "GET");
			return response;
		}

		if (!request.getPath().startsWith("/")) {
			return new HttpResponse(404);
		}

		File f = new File(securePath(request.getPath()));

		if (!f.exists() || !f.canRead()) {
			return new HttpResponse(404);
		}
		
		if (request.getHeader("If-Modified-Since") != null) {
			Date d = HttpUtils.parseDate(request.getHeader("If-Modified-Since"));
			if(d != null && d.compareTo(new Date(f.lastModified())) >= 0)
				return new HttpResponse(304);
		}

		HttpResponse response = new HttpResponse(200);

		if (f.isDirectory()) {
			File indexFile = new File(f, "/index.html");
			if(indexFile.exists() && indexFile.canRead() && !indexFile.isDirectory()) {
				f = indexFile;
			}
		}
		if(f.isDirectory()) {
				response.setData(renderDirectory(f, request.getPath()));
				response.addHeader("Content-Type", "text/html; charset=utf-8");
		} else {
			byte[] b = new byte[(int) f.length()];

			try {
				FileInputStream fis = new FileInputStream(f);
				fis.read(b);
			} catch (IOException e) {
				e.printStackTrace();
				return new HttpResponse(500);
			}
			
			response.setData(b);
			
			String mimeType = URLConnection.guessContentTypeFromName(f.getName());
			if (mimeType != null) {
				if (mimeType.startsWith("text/")) {
					mimeType += "; charset=utf-8";
				}
				response.addHeader("Content-Type", mimeType);
			}
		}

		response.addHeader("Last-Modified", HttpUtils.formatDate(
				new Date(f.lastModified())));

		return response;
	}

	private String securePath(String path) {
		if (File.pathSeparatorChar != '/')
			path = path.replace(File.pathSeparatorChar, '_');
		
		List<String> parts = Arrays.asList(path.split("/"));
		for (int i = 0; i < parts.size();) {
			if (parts.get(i).equals(".")) {
				parts.remove(i);
			} else if (parts.get(i).equals("..")) {
				parts.remove(i);
				if (i > 0)
					parts.remove(--i);
			} else {
				i++;
			}
		}

		path = "." + HttpUtils.join(parts, "/");
		if (File.separatorChar != '/')
			path = path.replace('/', File.separatorChar);

		return path;
	}

	private byte[] renderDirectory(File dir, String pathname) {
		if(!pathname.endsWith("/"))
			pathname += "/";
		
		StringBuilder buffer = new StringBuilder();
		
		buffer.append("<!DOCTYPE html>\n");
		buffer.append("<html lang=\"en\" xml:lang=\"en\"><head><title>Index of ");
		buffer.append(HttpUtils.escapeHtml(pathname));
		buffer.append("</title></head>\n");
		
		buffer.append("<body>\n");
		buffer.append("<h1>Index of ");
		buffer.append(HttpUtils.escapeHtml(pathname));
		buffer.append("</h1>\n");
		
		buffer.append("<table>\n");
		buffer.append("<tr><th>&nbsp;</th><th>Name</th><th>Last Modified</th><th>Size</th></tr>\n");
		
		File[] files = dir.listFiles();
		for(File f: files) {
			buffer.append("<tr><td>");
			if(f.isDirectory())
				buffer.append("[DIR]");
			buffer.append("</td><td><a href=\"");
			buffer.append(HttpUtils.escapeHtml(pathname));
			buffer.append(HttpUtils.escapeHtml(f.getName()));
			buffer.append("\">");
			buffer.append(HttpUtils.escapeHtml(f.getName()));
			buffer.append("</a></td><td>");
			buffer.append(HttpUtils.formatDate(new Date(f.lastModified())));
			buffer.append("</td><td>");
			buffer.append(f.length());
			buffer.append("</td></tr>\n");
		}
		
		buffer.append("</table>\n");
		buffer.append("Awesome HTTP-Server");
		buffer.append("</body></html>");

		try {
			return buffer.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
}
