package dev.slimevr.posestreamer;

import com.jme3.math.Vector3f;
import dev.slimevr.VRServer;
import dev.slimevr.tracking.processor.TransformNode;
import dev.slimevr.tracking.processor.skeleton.HumanSkeleton;
import solarxr_protocol.datatypes.BodyPart;

import java.io.*;
import java.net.*;


public class JsonPoseServer extends ServerPoseStreamer {
	DatagramSocket datagramSocket;
	InetAddress address;
	int port = 6788;

	public JsonPoseServer(VRServer server) {
		super(server);
		this.poseFileStream = new PoseDataStream(new ByteArrayOutputStream()) {
			OutputStreamWriter ow;

			@Override
			void writeFrame(HumanSkeleton skeleton) throws IOException {
				if (skeleton == null)
					return;
				ByteArrayOutputStream bo = (ByteArrayOutputStream) this.outputStream;
				if (bo != null) {
					if (ow == null)
						ow = new OutputStreamWriter(bo);
					ow.flush();
					bo.reset();
					WriteNode(skeleton.getRootNode());
					ow.flush();
					onData(bo.toByteArray());
				}
			}

			void WriteVec(Vector3f vec, float scale) throws IOException {
				ow
					.write(
						"["
							+ vec.getX() * scale
							+ ","
							+ vec.getY() * scale
							+ ","
							+ vec.getZ() * scale
							+ "]"
					);
			}

			void WriteNode(TransformNode node) throws IOException {
				ow.write("{");

				ow.write("\"name\":\"");

				String name = node.getBoneType().toString();
				int bodyPart = node.getBoneType().bodyPart;
				if(bodyPart != BodyPart.NONE)
					name += "->" + BodyPart.name(bodyPart);
				ow.write(name.replaceAll("\"", "\\\""));
				ow.write("\",\"localTrans\":");
				WriteVec(node.localTransform.getTranslation(), 1);
				ow.write(",\"worldTrans\":");
				WriteVec(node.worldTransform.getTranslation(), 1);
				ow.write(",\"childs\":[");
				boolean first = true;
				for (TransformNode child : node.children) {
					if (first)
						first = false;
					else
						ow.write(",");
					WriteNode(child);
				}
				ow.write("]");
				ow.write("}");
			}
		};
		try {
			datagramSocket = new DatagramSocket();
			address = InetAddress.getByName("localhost");
		} catch (SocketException | UnknownHostException e) {
			e.printStackTrace();
		}
	}

	void onData(byte[] data) {
		if (datagramSocket != null && address != null) {
			DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
			try {
				datagramSocket.send(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
