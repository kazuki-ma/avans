package me.geso.avans;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import lombok.NonNull;
import lombok.SneakyThrows;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class represents HTTP request. The object isn't thread safe. You
 * shouldn't share this object between threads.
 * 
 * @author tokuhirom
 *
 */
public class AvansRequest {
	private final HttpServletRequest request;
	private Map<String, List<FileItem>> uploads;
	private Map<String, String[]> parameters;

	public AvansRequest(final HttpServletRequest request) {
		try {
			request.setCharacterEncoding(this.getCharacterEncoding());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		this.request = request;
	}

	/**
	 * Get PATH_INFO.
	 * 
	 * @return
	 */
	public String getPathInfo() {
		return this.request.getPathInfo();
	}

	/**
	 * Get header string.
	 * 
	 * @param name
	 * @return
	 */
	public String getHeader(String name) {
		return this.request.getHeader(name);
	}

	/**
	 * Get all header values by name.
	 * 
	 * @param name
	 * @return
	 */
	public List<String> getHeaders(String name) {
		return Collections.list(this.request.getHeaders(name));
	}

	/**
	 * Get all headers in Map.
	 * 
	 * @return
	 */
	public Map<String, List<String>> getHeaderMap() {
		Map<String, List<String>> map = new TreeMap<>();
		Enumeration<String> headerNames = this.request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			final String name = headerNames.nextElement();
			final ArrayList<String> values = Collections.list(this.request
					.getHeaders(name));
			map.put(name, values);
		}
		return map;
	}

	/**
	 * Get CONTENT_LENGTH.
	 * 
	 * @return
	 */
	public int getContentLength() {
		return this.request.getContentLength();
	}

	/**
	 * Get HTTP_METHOD
	 * 
	 * @return
	 */
	public String getMethod() {
		return this.request.getMethod();
	}

	/**
	 * Returns the part of this request's URL from the protocol name up to the
	 * query string in the first line of the HTTP request. The web container
	 * does not decode this String. For example:
	 * 
	 * <table summary="Examples of Returned Values">
	 * <tr align=left>
	 * <th>First line of HTTP request</th>
	 * <th>Returned Value</th>
	 * <tr>
	 * <td>POST /some/path.html HTTP/1.1
	 * <td>
	 * <td>/some/path.html
	 * <tr>
	 * <td>GET http://foo.bar/a.html HTTP/1.0
	 * <td>
	 * <td>/a.html
	 * <tr>
	 * <td>HEAD /xyz?a=b HTTP/1.1
	 * <td>
	 * <td>/xyz
	 * </table>
	 *
	 * <p>
	 *
	 * @return a <code>String</code> containing the part of the URL from the
	 *         protocol name up to the query string
	 */
	public String getRequestURI() {
		return this.request.getRequestURI();
	}

	/**
	 * Get QUERY_STRING.
	 * 
	 * @return
	 */
	public String getQueryString() {
		return this.request.getQueryString();
	}

	/**
	 * Get cooies.
	 * 
	 * @return
	 */
	public Cookie[] getCookies() {
		return this.request.getCookies();
	}

	/**
	 * Get session object.
	 * 
	 * @return
	 */
	public HttpSession getSession() {
		return this.request.getSession();
	}

	/**
	 * Change session id.
	 */
	public void changeSessionId() {
		this.request.changeSessionId();
	}

	/**
	 * Read JSON from content-body. And parse it. This method runs hibernate
	 * validator. If the validation was failed, it throws runtime exception.
	 * 
	 * @param typeReference
	 * @return
	 */
	@SneakyThrows
	public <T> T readJSON(@NonNull final TypeReference<T> typeReference) {
		ServletInputStream inputStream = this.request.getInputStream();

		ObjectMapper mapper = new ObjectMapper();
		T instance = mapper.readValue(inputStream, typeReference);
		if (instance != null) {
			this.validate(instance);
			return instance;
		} else {
			throw new RuntimeException("null found... in content body");
		}
	}

	/**
	 * Read JSON from content-body. And parse it. This method runs hibernate
	 * validator. If the validation was failed, it throws runtime exception.
	 * 
	 * @param klass
	 * @return
	 */
	@SneakyThrows
	public <T> T readJSON(@NonNull final Class<T> klass) {
		ServletInputStream inputStream = this.request.getInputStream();

		ObjectMapper mapper = new ObjectMapper();
		T instance = mapper.readValue(inputStream, klass);
		if (instance != null) {
			this.validate(instance);
			return instance;
		} else {
			throw new RuntimeException("null found... in content body");
		}
	}

	/**
	 * Validate object by bean validation.
	 * 
	 * @param o
	 */
	public <T> void validate(T o) {
		ValidatorFactory validatorFactory = Validation
				.buildDefaultValidatorFactory();
		Validator validator = validatorFactory.getValidator();
		Set<ConstraintViolation<T>> violations = validator.validate(o);
		if (!violations.isEmpty()) {
			List<String> messages = new ArrayList<>();
			for (ConstraintViolation<T> violation: violations) {
				String message = violation.getPropertyPath() + " " + violation.getMessage();
				messages.add(message);
			}
			throw new AvansValidationException(messages);
		}
	}

	/**
	 * Read parameters from query string/content-body
	 * 
	 * @param name
	 * @return
	 */
	public Optional<String> getParameter(String name) {
		String[] strings = this.getParameterMap().get(name);
		if (strings == null) {
			return Optional.empty();
		}
		if (strings.length == 0) {
			return Optional.empty();
		}
		return Optional.of(strings[0]);
	}

	/**
	 * Get parameters from queryString/contentBody by name.
	 * 
	 * @param name
	 * @return
	 */
	public List<String> getParameters(String name) {
		String[] strings = this.getParameterMap().get(name);
		if (strings == null) {
			return new ArrayList<>();
		} else {
			return Arrays.asList(strings);
		}
	}

	protected String getCharacterEncoding() {
		return "UTF-8";
	}

	/**
	 * Get int value from parameter.
	 * 
	 * @param name
	 * @return
	 */
	public OptionalInt getIntParam(String name) {
		Optional<String> parameter = this.getParameter(name);
		if (parameter.isPresent()) {
			return OptionalInt.of(Integer.parseInt(parameter.get()));
		} else {
			return OptionalInt.empty();
		}
	}

	/**
	 * Get uploaded file object by name.
	 * 
	 * @param name
	 * @return
	 */
	public Optional<FileItem> getFileItem(String name) {
		List<FileItem> items = this.getFileItemMap().get(name);
		if (items == null || items.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(items.get(0));
	}

	/**
	 * Get uploaded file items by name.
	 * 
	 * @param name
	 * @return
	 */
	public List<FileItem> getFileItems(String name) {
		List<FileItem> items = this.getFileItemMap().get(name);
		if (items == null) {
			return new ArrayList<>();
		}
		return items;
	}

	/**
	 * Get all uploaded file items.
	 * 
	 * @return
	 */
	public Map<String, List<FileItem>> getFileItemMap() {
		this.getParameterMap(); // initialize this.uploads
		return this.uploads;
	}

	/**
	 * Get parameters in Map. This method parses followings.
	 * <ul>
	 * <li>query string</li>
	 * <li>contentBody: multipart/form-data</li>
	 * <li>contentBody: application/x-www-form-urlencoded</li>
	 * </ul>
	 * 
	 * @return
	 */
	public Map<String, String[]> getParameterMap() {
		if (this.parameters == null) {
			try {
				this.parameters = new TreeMap<>(request.getParameterMap());

				if (ServletFileUpload.isMultipartContent(request)) {
					ServletFileUpload servletFileUpload = this.createServletFileUpload();
					List<FileItem> fileItems = servletFileUpload
							.parseRequest(this.request);
					this.uploads = new TreeMap<>();
					for (FileItem fileItem : fileItems) {
						if (fileItem.isFormField()) {
							String value = fileItem.getString(this
									.getCharacterEncoding());
							String[] strings = new String[1];
							strings[0] = value;
							this.parameters.put(fileItem.getFieldName(),
									strings);
						} else {
							if (this.uploads.containsKey(fileItem
									.getFieldName())) {
								this.uploads.get(fileItem.getFieldName()).add(
										fileItem);
							} else {
								List<FileItem> list = new ArrayList<>();
								list.add(fileItem);
								this.uploads.put(fileItem.getFieldName(), list);
							}
						}
					}
				}
			} catch (FileUploadException | UnsupportedEncodingException e) {
				this.parameters = null;
				throw new RuntimeException(e);
			}
		}
		return this.parameters;
	}

	/**
	 * Create new ServletFileUpload instance.
	 * You can override this method.
	 *
	 * <p>See also commons-fileupload.</p>
	 * 
	 * @return
	 */
	protected ServletFileUpload createServletFileUpload() {
		FileItemFactory fileItemFactory = new DiskFileItemFactory();
		return new ServletFileUpload(fileItemFactory);
	}

}
