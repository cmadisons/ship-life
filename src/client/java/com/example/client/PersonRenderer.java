package com.example.client;

import com.example.Person;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Drawing a person.
 *
 * They hang on the plain humanoid model -- the same head, body, two arms and
 * two legs the player is built from -- so a skin drawn for a player fits one
 * of these without a single measurement changing.
 *
 * Which skin is chosen by the label over their head and the colour of it,
 * both of which the client already has for drawing the label -- so nobody had
 * to invent a second way of saying who somebody is. The colour is what tells
 * two people with the same job apart: both friends are labelled Friend, and
 * Ben's is aqua where Izzy's is purple. Anyone new gets the front desk
 * uniform, which is the right guess on a ship.
 */
public class PersonRenderer extends HumanoidMobRenderer<Person, HumanoidRenderState,
		HumanoidModel<HumanoidRenderState>> {

	private static final String FOLDER = "textures/entity/person/";

	public PersonRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		String who = state.nameTag == null ? "" : state.nameTag.getString().toLowerCase();
		String skin = switch (who) {
			case "charlie" -> "charlie";
			case "ben" -> "ben";
			case "izzy" -> "izzy";
			case "maria" -> "staff_one";
			case "gus" -> "cook";
			default -> "staff_two";
		};
		return Identifier.fromNamespaceAndPath("shiplife", FOLDER + skin + ".png");
	}
}
