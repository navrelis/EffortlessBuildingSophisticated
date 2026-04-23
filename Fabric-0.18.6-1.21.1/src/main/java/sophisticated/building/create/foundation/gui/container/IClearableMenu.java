package sophisticated.building.create.foundation.gui.container;

public interface IClearableMenu {

	default void sendClearPacket() {
	}

	public void clearContents();

}
