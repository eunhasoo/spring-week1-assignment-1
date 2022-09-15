package com.codesoom.assignment.handler;

import com.codesoom.assignment.http.HttpStatus;
import com.codesoom.assignment.http.JsonParser;
import com.codesoom.assignment.http.RequestMethod;
import com.codesoom.assignment.http.RequestUtils;
import com.codesoom.assignment.http.ResponseUtils;
import com.codesoom.assignment.model.Task;
import com.codesoom.assignment.repository.TaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URI;

public class TaskHttpHandler implements HttpHandler {

    private final TaskRepository taskRepository;

    public TaskHttpHandler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String requestURI = uri.getPath();
        String requestBody = RequestUtils.readRequestBody(exchange.getRequestBody());
        RequestMethod requestMethod = RequestMethod.valueOf(exchange.getRequestMethod());

        if (!RequestUtils.isValidRequest(requestMethod, requestURI, requestBody)) {
            ResponseUtils.sendError(exchange, HttpStatus.BAD_REQUEST);
        }

        Long id = RequestUtils.getResourceId(requestURI).orElse(null);
        switch (requestMethod) {
            case GET:
                listTodos(exchange, id);
                break;
            case POST:
                createTodo(exchange, requestBody);
                break;
            case PUT:
            case PATCH:
                updateTodo(exchange, id, requestBody);
                break;
            case DELETE:
                deleteTodo(exchange, id);
                break;
            default:
                throw new IllegalStateException("처리할 수 없는 잘못된 요청입니다.");
        }
    }

    private void listTodos(HttpExchange exchange, Long id) throws IOException {
        try {
            String json;
            if (id == null) {
                json = JsonParser.toJSON(taskRepository.getTasks());
            } else {
                Task task = taskRepository.getTaskById(id);
                json = JsonParser.toJSON(task);
            }
            ResponseUtils.sendResponse(exchange, json, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            ResponseUtils.sendError(exchange, HttpStatus.NOT_FOUND);
        }
    }

    private void createTodo(HttpExchange exchange, String requestBody) throws IOException {
        try {
            Task newTask = JsonParser.toTask(requestBody);
            taskRepository.addTask(newTask);
            String taskJson = JsonParser.toJSON(newTask);
            ResponseUtils.sendResponse(exchange, taskJson, HttpStatus.CREATED);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateTodo(HttpExchange exchange, Long id, String requestBody) throws IOException {
        Task task = JsonParser.toTask(requestBody);
        try {
            Task updatedTask = taskRepository.updateTask(id, task);
            String json = JsonParser.toJSON(updatedTask);
            ResponseUtils.sendResponse(exchange, json, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            ResponseUtils.sendError(exchange, HttpStatus.NOT_FOUND);
        }
    }

    private void deleteTodo(HttpExchange exchange, Long id) throws IOException {
        boolean isDeleted = taskRepository.deleteTask(id);
        if (isDeleted) {
            ResponseUtils.sendResponse(exchange, "Task is deleted.", HttpStatus.NO_CONTENT);
        } else {
            ResponseUtils.sendError(exchange, HttpStatus.NOT_FOUND);
        }
    }
}
