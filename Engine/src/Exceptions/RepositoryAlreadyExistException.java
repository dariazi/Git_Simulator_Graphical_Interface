package Exceptions;

public class RepositoryAlreadyExistException extends Exception {
    private final String EXCEPTION_MESSAGE = "%s already exists";
    private String path;

    public RepositoryAlreadyExistException(String i_path) {
        path = i_path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String getMessage() {
        return String.format(EXCEPTION_MESSAGE, path);
    }
}
