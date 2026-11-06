package jp.nw.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.dao.ThreadDao;
import jp.nw.dto.ThreadCommentDto;
import jp.nw.dto.ThreadDto;
import jp.nw.entity.UserEntity;
import jp.nw.domain.user.PermissionAction;
import jp.nw.util.PermissionCheckUtil;
import jp.nw.util.PageJson;
import jp.nw.model.AiResponseJobLogic;

@WebServlet("/ThreadDetailController")
public class ThreadDetailController extends HttpServlet {

        private static final long serialVersionUID = 1L;

        protected void doGet(
                        HttpServletRequest request,
                        HttpServletResponse response)
                        throws ServletException, IOException {

                String idValue = request.getParameter("id");
                if (idValue == null) idValue = request.getParameter("threadId");
                int threadId;
                try {
                        threadId = Integer.parseInt(idValue);
                } catch (Exception e) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                }

                ThreadDao dao = new ThreadDao();

                ThreadDto thread = dao.findById(threadId);
                if (thread == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                }

                List<ThreadCommentDto> commentList = dao.findComments(threadId);

                HttpSession session = request.getSession();
                UserEntity loginUser = (UserEntity) session.getAttribute(
                                "loginUser");
                boolean canManageThread = loginUser.getUserId().equals(thread.getAuthorId())
                                || PermissionCheckUtil.can(loginUser, PermissionAction.THREAD_MODERATE_ANY);
                if (session.getAttribute("threadCsrfToken") == null) {
                        session.setAttribute("threadCsrfToken", UUID.randomUUID().toString());
                }
                request.setAttribute("threadFlash", session.getAttribute("threadFlash"));
                request.setAttribute("threadFlashType", session.getAttribute("threadFlashType"));
                session.removeAttribute("threadFlash");
                session.removeAttribute("threadFlashType");

                PageJson.set(request, "threadJson", new ThreadView(thread.getThreadId(), thread.getTitle(),
                                thread.getAuthorId(), thread.getThreadContent(), thread.getStatus(), thread.isClosed(),
                                thread.getClosedById(), thread.getClosedAt() == null ? null : thread.getClosedAt().toString(),
                                canManageThread, commentList.stream().map(comment -> new CommentView(comment.getCommentId(),
                                                comment.getAuthorId(), comment.getCommentText(),
                                                comment.getCreatedAt() == null ? null : comment.getCreatedAt().toString(),
                                                loginUser.getUserId().equals(comment.getAuthorId()),
                                                "AI".equals(comment.getAuthorAccountType()))).toList()));
                request.setAttribute("aiResponsePending", AiResponseJobLogic.hasPending("THREAD", String.valueOf(threadId)));

                RequestDispatcher dispatcher = request.getRequestDispatcher(
                                "/WEB-INF/jsp/thread/ThreadDetail.jsp");

                dispatcher.forward(request, response);
        }

        private record ThreadView(int id, String title, String authorId, String content, String status, boolean closed,
                        String closedById, String closedAt, boolean canManage, List<CommentView> comments) {
        }

        private record CommentView(int id, String authorId, String text, String createdAt, boolean mine, boolean ai) {
        }
}
