package jp.nw.application;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.base.ApplicationCommand;
import jp.nw.entity.ChatMessageEntity;
import jp.nw.entity.ChatRoomEntity;
import jp.nw.entity.UserEntity;
import jp.nw.model.ChatOpenLogic;
import jp.nw.model.RegistrationChatLogic;

public class RegistrationChatCommand extends ApplicationCommand {
    
    private HttpServletRequest request = null;
	private HttpServletResponse response = null;

    private UserEntity userEntity = null;

    private List<ChatRoomEntity> chatRoomList;
    private List<ChatMessageEntity> chatMessageList;

    private String roomId = "";
    private String comment = "";

	private static final String KEY_SESSIONUSER = "loginUser";
	private static final String KEY_REQ = "request";
	private static final String KEY_RES = "response";
    private static final String KEY_CHATCHANELS = "chatchanels";

	public boolean setCommandData(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
		return true;
	}

	protected boolean doCommandData() {
		try {
			// パラメータ取得
			request.setCharacterEncoding("UTF-8");

			HttpSession session = this.request.getSession();
			this.userEntity = (UserEntity) session.getAttribute(KEY_SESSIONUSER);
            this.roomId = this.request.getParameter("roomId");
            this.comment = this.request.getParameter("commentText");

			return true;
		} catch (Exception e) {
			this.logger.writeInfo("Parameter Error");
			return false;
		}

	}

	protected boolean executeCommand() {
		try {
			// チャットの登録
			RegistrationChatLogic logic = new RegistrationChatLogic();
			long retId = logic.register(this.userEntity, this.roomId, this.comment);

            if (retId == -1L) {
                return false;
            }

            ChatOpenLogic retChatLogic = new ChatOpenLogic();
            this.chatMessageList = retChatLogic.getChanelOpen(roomId, request);
			return true;
		} catch (Exception e) {
			this.logger.writeInfo("");
			return false;
		}
	}

	protected boolean commandOutput() {
		this.output.setValue(KEY_REQ, this.request);
		this.output.setValue(KEY_RES, this.response);

        this.output.setValue(KEY_REQ, logger);
        this.output.setValue("ChatMessageList", this.chatMessageList);

		return true;
	}
}
