/**
 * This file is part of lavagna.
 *
 * lavagna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * lavagna is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with lavagna.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lavagna.web.api;

import io.lavagna.model.BoardColumn;
import io.lavagna.model.BoardColumnDefinition;
import io.lavagna.model.BoardInfo;
import io.lavagna.model.ColumnDefinition;
import io.lavagna.model.Permission;
import io.lavagna.model.Project;
import io.lavagna.model.ProjectMetadata;
import io.lavagna.model.UserWithPermission;
import io.lavagna.model.util.ShortNameGenerator;
import io.lavagna.service.BoardColumnRepository;
import io.lavagna.service.BoardRepository;
import io.lavagna.service.EventEmitter;
import io.lavagna.service.ExcelExportService;
import io.lavagna.service.ProjectService;
import io.lavagna.service.SearchService;
import io.lavagna.service.StatisticsService;
import io.lavagna.web.api.model.CreateRequest;
import io.lavagna.web.api.model.Suggestion;
import io.lavagna.web.api.model.TaskStatistics;
import io.lavagna.web.api.model.TaskStatisticsAndHistory;
import io.lavagna.web.api.model.UpdateRequest;
import io.lavagna.web.api.model.ValidationException;
import io.lavagna.web.helper.ExpectPermission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectController {

    private final ProjectService projectService;
    private final BoardRepository boardRepository;
    private final EventEmitter eventEmitter;
    private final StatisticsService statisticsService;
    private final SearchService searchService;
    private final BoardColumnRepository boardColumnRepository;
    private final ExcelExportService excelExportService;

    @Autowired
    public ProjectController(ProjectService projectService, BoardRepository boardRepository, EventEmitter eventEmitter,
        StatisticsService statisticsService, SearchService searchService, BoardColumnRepository boardColumnRepository,
        ExcelExportService excelExportService) {
        this.projectService = projectService;
        this.boardRepository = boardRepository;
        this.eventEmitter = eventEmitter;
        this.statisticsService = statisticsService;
        this.searchService = searchService;
        this.boardColumnRepository = boardColumnRepository;
        this.excelExportService = excelExportService;
    }

    @RequestMapping(value = "/api/project", method = RequestMethod.GET)
    public List<Project> findProjects(UserWithPermission user) {
        return projectService.findAllProjects(user);
    }

    @ExpectPermission(Permission.ADMINISTRATION)
    @RequestMapping(value = "/api/project", method = RequestMethod.POST)
    public void create(@RequestBody CreateRequest project) {
        checkShortName(project.getShortName());
        projectService.create(project.getName(), project.getShortName(), project.getDescription());
        eventEmitter.emitCreateProject(project.getShortName());
    }

    @ExpectPermission(Permission.ADMINISTRATION)
    @RequestMapping(value = "/api/suggest-project-short-name", method = RequestMethod.GET)
    public Suggestion suggestProjectShortName(@RequestParam("name") String name) {
        return new Suggestion(ShortNameGenerator.generateShortNameFrom(name));
    }

    @ExpectPermission(Permission.ADMINISTRATION)
    @RequestMapping(value = "/api/check-project-short-name", method = RequestMethod.GET)
    public boolean checkProjectShortName(@RequestParam("name") String name) {
        return ShortNameGenerator.isShortNameValid(name) && !projectService.existsWithShortName(name);
    }

    @ExpectPermission(Permission.READ)
    @RequestMapping(value = "/api/project/{projectShortName}", method = RequestMethod.GET)
    public Project findByShortName(@PathVariable("projectShortName") String shortName) {
        return projectService.findByShortName(shortName);
    }

    @ExpectPermission(Permission.READ)
    @RequestMapping(value = "/api/project/{projectShortName}/definitions", method = RequestMethod.GET)
    public List<BoardColumnDefinition> findColumnDefinitions(@PathVariable("projectShortName") String shortName) {
        Project project = projectService.findByShortName(shortName);
        return projectService.findColumnDefinitionsByProjectId(project.getId());
    }

    @ExpectPermission(Permission.READ)
    @RequestMapping(value = "/api/project/{projectShortName}/board", method = RequestMethod.GET)
    public List<BoardInfo> findBoards(@PathVariable("projectShortName") String shortName) {
        Project project = projectService.findByShortName(shortName);
        return boardRepository.findBoardInfo(project.getId());
    }

    @ExpectPermission(Permission.READ)
    @RequestMapping(value = "/api/project/{projectShortName}/metadata", method = RequestMethod.GET)
    public ProjectMetadata getMetadata(@PathVariable("projectShortName") String shortName) {
        return projectService.getMetadata(shortName);
    }

    @ExpectPermission(Permission.PROJECT_ADMINISTRATION)
    @RequestMapping(value = "/api/project/{projectShortName}/board", method = RequestMethod.POST)
    public void createBoard(@PathVariable("projectShortName") String shortName, @RequestBody CreateRequest board) {
        checkShortName(board.getShortName());
        Project project = projectService.findByShortName(shortName);
        boardRepository.createNewBoard(board.getName(), board.getShortName(), board.getDescription(), project.getId());
        eventEmitter.emitCreateBoard(project.getShortName());
    }

    @ExpectPermission(Permission.PROJECT_ADMINISTRATION)
    @RequestMapping(value = "/api/project/{projectShortName}/definition", method = RequestMethod.PUT)
    public int updateColumnDefinition(@PathVariable("projectShortName") String shortName,
        @RequestBody UpdateColumnDefinition columnDefinition) {
        Project project = projectService.findByShortName(shortName);
        return projectService.updateColumnDefinition(project.getId(), columnDefinition.getDefinition(),
            columnDefinition.getColor());
    }

    @ExpectPermission(Permission.PROJECT_ADMINISTRATION)
    @RequestMapping(value = "/api/project/{projectShortName}", method = RequestMethod.POST)
    public Project updateProject(@PathVariable("projectShortName") String shortName,
        @RequestBody UpdateRequest updatedProject) {
        Project project = projectService.findByShortName(shortName);
        project = projectService.updateProject(project.getId(), updatedProject.getName(),
            updatedProject.getDescription(), updatedProject.isArchived());
        eventEmitter.emitUpdateProject(shortName);
        return project;
    }

    @ExpectPermission(Permission.READ)
    @RequestMapping(value = "/api/project/{projectShortName}/task-statistics", method = RequestMethod.GET)
    public TaskStatistics projectTaskStatistics(@PathVariable("projectShortName") String projectShortName,
        UserWithPermission user) {
        int projectId = projectService.findByShortName(projectShortName).getId();

        Map<ColumnDefinition, Integer> tasks = searchService
            .findTaksByColumnDefinition(projectId, null, true, user);

        Map<ColumnDefinition, BoardColumnDefinition> columnDefinitions = projectService
            .findMappedColumnDefinitionsByProjectId(projectId);

        return new TaskStatistics(tasks, columnDefinitions);
    }

    @ExpectPermission(Permission.READ)
    @RequestMapping(value = "/api/project/{projectShortName}/statistics/{fromDate}", method = RequestMethod.GET)
    public TaskStatisticsAndHistory projectStatistics(@PathVariable("projectShortName") String projectShortName,
        @PathVariable("fromDate") Date fromDate, UserWithPermission user) {
        int projectId = projectService.findByShortName(projectShortName).getId();

        Map<ColumnDefinition, Integer> tasks = searchService
            .findTaksByColumnDefinition(projectId, null, true, user);

        Map<ColumnDefinition, BoardColumnDefinition> columnDefinitions = projectService
            .findMappedColumnDefinitionsByProjectId(projectId);

        Map<Long, Map<ColumnDefinition, Long>> cardStatus = statisticsService.getCardsStatusByProject(projectId,
            fromDate);

        Integer activeUsers = statisticsService.getActiveUsersOnProject(projectId, fromDate);

        return new TaskStatisticsAndHistory(tasks, columnDefinitions, cardStatus,
            statisticsService.getCreatedAndClosedCardsByProject(projectId, fromDate),
            activeUsers,
            statisticsService.getAverageUsersPerCardOnProject(projectId),
            statisticsService.getAverageCardsPerUserOnProject(projectId),
            statisticsService.getCardsByLabelOnProject(projectId),
            statisticsService.getMostActiveCardByProject(projectId, fromDate));
    }

    @ExpectPermission(Permission.READ)
    @RequestMapping(value = "/api/project/{projectShortName}/export/", method = RequestMethod.GET)
    public void exportMilestoneToExcel(@PathVariable("projectShortName") String projectShortName,
        UserWithPermission user, HttpServletResponse resp)
        throws IOException {

        HSSFWorkbook wb = excelExportService.exportProjectToExcel(projectShortName, user);

        resp.setHeader("Content-disposition", "attachment; filename=" + projectShortName + ".xls");
        wb.write(resp.getOutputStream());
    }

    /**
     * Check the format of a short name.
     * <p/>
     * It must be composed only with uppercase alpha numeric ASCII characters and "_".
     *
     * @param shortName
     */
    private static void checkShortName(String shortName) {
        if (!ShortNameGenerator.isShortNameValid(shortName)) {
            throw new ValidationException();
        }
    }

    @ExpectPermission(Permission.READ)
    @RequestMapping(value = "/api/project/{shortName}/columns-in/", method = RequestMethod.GET)
    public List<ProjectColumn> fetchAllColumns(@PathVariable("shortName") String shortName) {

        List<ProjectColumn> list = new ArrayList<>();

        Project project = projectService.findByShortName(shortName);
        for (BoardInfo bInfo : boardRepository.findBoardInfo(project.getId())) {
            int boardId = boardRepository.findBoardIdByShortName(bInfo.getShortName());
            for (BoardColumn bc : boardColumnRepository.findAllColumnsFor(boardId)) {
                if (bc.getLocation().equals(BoardColumn.BoardColumnLocation.BOARD)) {
                    list.add(new ProjectColumn(bInfo.getName(), bc.getId(), bc.getName()));
                }
            }
        }
        return list;
    }

    @Getter
    @Setter
    public static class UpdateColumnDefinition {
        private int definition;
        private int color;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ProjectColumn {
        private String board;
        private int columnId;
        private String columnName;
    }
}
