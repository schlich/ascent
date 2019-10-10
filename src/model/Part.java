package model;

import com.comsol.model.GeomFeature;
import com.comsol.model.Model;
import com.comsol.model.ModelParam;
import com.comsol.nativejni.geom.Geom;

import java.util.HashMap;

class Part {

    private static void examples(ModelWrapper2 mw) {

        Model model = mw.getModel();

        // assume this is passed in (i.e. "id" in method below)
        String partID = "pi1";

        //  id's and pseudonyms
        String wpPseudo = "MY_WORKPLANE";
        String wpID = mw.next("wp", wpPseudo);

        String swePseudo = "MY_SWEEP"; // I ~think~ it's a sweep?
        String sweID = mw.next("swe", swePseudo);

        String cselPseudo = "MY_CSEL";
        String cselID = mw.next("csel", cselPseudo);

        // create those items
        model.geom(partID).create(wpID, "WorkPlane");
        model.geom(partID).create(sweID, "Sweep");
        model.geom(partID).selection().create(cselID, "CumulativeSelection");

        // one mechanic, but not sure if this is how it actually works
        // assuming this is first wp1_csel, should have the name "wp1_csel1"
        model.geom(partID).feature(sweID).selection("face").named(mw.next(wpID + "_csel"));

        // other possible mechanic: that name is just referring to already existing objects
        model.geom(partID).feature(sweID).selection("face").named(wpID + "_" + cselID);

        // also, other new thing: just instantiate a new CMI if need  to restart indexing for part
        IdentifierManager thisPartIM = new IdentifierManager();
        String restartedIDwp = thisPartIM.next("wp");

    }


    public static IdentifierManager createPartPrimitive(String id, String pseudonym, ModelWrapper2 mw) throws IllegalArgumentException {
        return Part.createPartPrimitive(id, pseudonym, mw, null);
    }

//    public static boolean createPartInstance(String id, String pseudonym, ModelWrapper2 mw,
//                                             HashMap<String, String> partPrimitives) {
//        return createPartInstance(id, pseudonym, mw, partPrimitives, null);
//    }

    /**
     *
     * @param id
     * @param pseudonym
     * @param mw
     * @param data
     * @param data
     * @return
     */
    public static IdentifierManager createPartPrimitive(String id, String pseudonym, ModelWrapper2 mw,
                                              HashMap<String, Object> data) throws IllegalArgumentException {


        Model model = mw.getModel();

        model.geom().create(id, "Part", 3);
        model.geom(id).label(pseudonym);
        model.geom(id).lengthUnit("\u00b5m");

        // only used once per method, so ok to define outside the switch
        IdentifierManager im = new IdentifierManager();
        ModelParam mp = model.geom(id).inputParam();

        switch (pseudonym) {
            case "TubeCuff_Primitive":
                mp.set("N_holes", "1");
                mp.set("Theta", "340 [deg]");
                mp.set("Center", "10 [mm]");
                mp.set("R_in", "1 [mm]");
                mp.set("R_out", "2 [mm]");
                mp.set("L", "5 [mm]");
                mp.set("Rot_def", "0 [deg]");
                mp.set("D_hole", "0.3 [mm]");
                mp.set("Buffer_hole", "0.1 [mm]");
                mp.set("L_holecenter_cuffseam", "0.3 [mm]");
                mp.set("Pitch_holecenter_holecenter", "0 [mm]");

                String[] cselTCLabels = {
                        "INNER CUFF SURFACE",
                        "OUTER CUFF SURFACE", "CUFF FINAL",
                        "CUFF wGAP PRE HOLES",
                        "CUFF PRE GAP",
                        "CUFF PRE GAP PRE HOLES",
                        "CUFF GAP CROSS SECTION",
                        "CUFF GAP",
                        "CUFF PRE HOLES",
                        "HOLE 1",
                        "HOLE 2",
                        "HOLES"
                };
                for (String cselTCLabel: cselTCLabels) {
                    model.geom(id).selection().create(im.next("csel", cselTCLabel), "CumulativeSelection")
                            .label(cselTCLabel);
                }

                String micsLabel = "Make Inner Cuff Surface";
                GeomFeature inner_surf = model.geom(id).create(im.next("cyl",micsLabel), "Cylinder");
                inner_surf.label(micsLabel);
                inner_surf.set("contributeto", im.get(cselTCLabels[0]));
                inner_surf.set("pos", new String[]{"0", "0", "Center-(L/2)"});
                inner_surf.set("r", "R_in");
                inner_surf.set("h", "L");

                String mocsLabel = "Make Outer Cuff Surface";
                GeomFeature outer_surf = model.geom(id).create(im.next("cyl",mocsLabel),"Cylinder");
                outer_surf.label(mocsLabel);
                outer_surf.set("contributeto", mw.get("OUTER CUFF SURFACE"));
                outer_surf.set("pos", new String[]{"0", "0", "Center-(L/2)"});
                outer_surf.set("r", "R_out");
                outer_surf.set("h", "L");

                String ifgnhLabel = "If (No Gap AND No Holes)";
                GeomFeature if_gap_no_holes = model.geom(id).create(im.next("if", ifgnhLabel), "If");
                if_gap_no_holes.label(ifgnhLabel);
                if_gap_no_holes.set("condition", "(Theta==360) && (N_holes==0)");

                String difrdwicsLabel = "Remove Domain Within Inner Cuff Surface";
                GeomFeature dif_remove_ics = model.geom(id).create(im.next("dif", difrdwicsLabel), "Difference");
                dif_remove_ics.label(difrdwicsLabel);
                dif_remove_ics.set("contributeto", im.get("CUFF FINAL"));
                dif_remove_ics.selection("input").named(im.get("OUTER CUFF SURFACE"));
                dif_remove_ics.selection("input2").named(im.get("INNER CUFF SURFACE"));

                String elseifganhLabel = "If (Gap AND No Holes)";
                GeomFeature elseif_gap_noholes = model.geom(id).create(im.next("elseif",elseifganhLabel), "ElseIf");
                elseif_gap_noholes.label(elseifganhLabel);
                elseif_gap_noholes.set("condition", "(Theta<360) && (N_holes==0)");

                String difrmwics1Label = "Remove Domain Within Inner Cuff Surface 1";
                GeomFeature dif_remove_ics1 = model.geom(id).create(im.next("dif",difrmwics1Label), "Difference");
                dif_remove_ics1.label(difrmwics1Label);
                dif_remove_ics1.set("contributeto", im.get("CUFF PRE GAP"));
                dif_remove_ics1.selection("input").named(im.get("OUTER CUFF SURFACE"));
                dif_remove_ics1.selection("input2").named(im.get("INNER CUFF SURFACE"));

                String wpmcgcsLabel = "Make Cuff Gap Cross Section";
                GeomFeature wp_make_cuffgapcx = model.geom(id).create(im.next("wp",wpmcgcsLabel), "WorkPlane");
                wp_make_cuffgapcx.label(wpmcgcsLabel);
                wp_make_cuffgapcx.set("contributeto", im.get("CUFF GAP CROSS SECTION"));
                wp_make_cuffgapcx.set("quickplane", "xz");
                wp_make_cuffgapcx.set("unite", true);
                wp_make_cuffgapcx.geom().create("r1", "Rectangle");
                wp_make_cuffgapcx.geom().feature("r1").label("Cuff Gap Cross Section");
                wp_make_cuffgapcx.geom().feature("r1").set("pos", new String[]{"R_in+((R_out-R_in)/2)", "Center"});
                wp_make_cuffgapcx.geom().feature("r1").set("base", "center");
                wp_make_cuffgapcx.geom().feature("r1").set("size", new String[]{"R_out-R_in", "L"});

                String revmcgLabel = "Make Cuff Gap";
                GeomFeature rev_make_cuffgap = model.geom(id).create(im.next("rev",revmcgLabel), "Revolve");
                rev_make_cuffgap.label(revmcgLabel);
                rev_make_cuffgap.set("contributeto", im.get("CUFF GAP"));
                rev_make_cuffgap.set("angle1", "Theta");
                rev_make_cuffgap.selection("input").set(im.get("Make Cuff Gap Cross Section"));

                String difrcgLabel = "Remove Cuff Gap";
                GeomFeature dif_remove_cuffgap = model.geom(id).create(im.next("dif",difrcgLabel), "Difference");
                dif_remove_cuffgap.label(difrcgLabel);
                dif_remove_cuffgap.set("contributeto", im.get("CUFF FINAL"));
                dif_remove_cuffgap.selection("input").named(im.get("CUFF PRE GAP"));
                dif_remove_cuffgap.selection("input2").named(im.get("CUFF GAP"));

                String rotdc1Label = "Rotate to Default Conformation 1";
                GeomFeature rot_default_conformation1 = model.geom(id).create(im.next("rot",rotdc1Label), "Rotate");
                rot_default_conformation1.label(rotdc1Label);
                rot_default_conformation1.set("rot", "Rot_def");
                rot_default_conformation1.selection("input").named(im.get("CUFF FINAL"));

                String elifngnhLabel = "If (No Gap AND Holes)";
                GeomFeature elif_nogap_noholes = model.geom(id).create(im.next("elseif",elifngnhLabel), "ElseIf");
                elif_nogap_noholes.label(elifngnhLabel);
                elif_nogap_noholes.set("condition", "(Theta==360) && (N_holes>0)");

                String difrdwics2 = "Remove Domain Within Inner Cuff Surface 2";
                GeomFeature dif_remove_domain_inner_cuff2 = model.geom(id).create(im.next("dif",difrdwics2), "Difference");
                dif_remove_domain_inner_cuff2.label(difrdwics2);
                dif_remove_domain_inner_cuff2.set("contributeto", im.get("CUFF PRE HOLES"));
                dif_remove_domain_inner_cuff2.selection("input").named(im.get("OUTER CUFF SURFACE"));
                dif_remove_domain_inner_cuff2.selection("input2").named(im.get("INNER CUFF SURFACE"));

                String econmhsLabel = "Make Hole Shape";
                GeomFeature econ_make_holeshape = model.geom(id).create(im.next("econ",econmhsLabel), "ECone");
                econ_make_holeshape.label(econmhsLabel);
                econ_make_holeshape.set("contributeto", im.get("HOLES"));
                econ_make_holeshape.set("pos", new String[]{"R_in-Buffer_hole/2", "0", "Center+Pitch_holecenter_holecenter/2"});
                econ_make_holeshape.set("axis", new int[]{1, 0, 0});
                econ_make_holeshape.set("semiaxes", new String[]{"D_hole/2", "D_hole/2"});
                econ_make_holeshape.set("h", "(R_out-R_in)+Buffer_hole");
                econ_make_holeshape.set("rat", "R_out/R_in");

                String rotphicLabel = "Position Hole in Cuff";
                GeomFeature rot_pos_hole = model.geom(id).create(im.next("rot",rotphicLabel), "Rotate");
                rot_pos_hole.label(rotphicLabel);
                rot_pos_hole.set("rot", "(360*L_holecenter_cuffseam)/(pi*2*R_in)");
                rot_pos_hole.selection("input").named(im.get("HOLES"));

                String difmichLabel = "Make Inner Cuff Hole";
                GeomFeature dif_make_innercuff_hole = model.geom(id).create(im.next("dif",difmichLabel), "Difference");
                dif_make_innercuff_hole.label(difmichLabel);
                dif_make_innercuff_hole.set("contributeto", im.get("CUFF FINAL"));
                dif_make_innercuff_hole.selection("input").named(im.get("CUFF PRE HOLES"));
                dif_make_innercuff_hole.selection("input2").named(im.get("HOLES"));

                String elifgahLabel = "If (Gap AND Holes)";
                GeomFeature elif_gap_and_holes = model.geom(id).create(im.next("elseif",elifgahLabel), "ElseIf");
                elif_gap_and_holes.label(elifgahLabel);
                elif_gap_and_holes.set("condition", "(Theta<360) && (N_holes>0)");

                String difrdwics3Label = "Remove Domain Within Inner Cuff Surface 3";
                GeomFeature dif_remove_domain_inner_cuff3 = model.geom(id).create(im.next("dif",difrdwics3Label), "Difference");
                dif_remove_domain_inner_cuff3.label(difrdwics3Label);
                dif_remove_domain_inner_cuff3.set("contributeto", im.get("CUFF PRE GAP PRE HOLES"));
                dif_remove_domain_inner_cuff3.selection("input").named(im.get("OUTER CUFF SURFACE"));
                dif_remove_domain_inner_cuff3.selection("input2").named(im.get("INNER CUFF SURFACE"));

                String wpmcgcs1Label  = "Make Cuff Gap Cross Section 1";
                GeomFeature wp_make_cuffgapcx1 = model.geom(id).create(im.next("wp",wpmcgcs1Label), "WorkPlane");
                wp_make_cuffgapcx1.label(wpmcgcs1Label);
                wp_make_cuffgapcx1.set("contributeto", im.get("CUFF GAP CROSS SECTION"));
                wp_make_cuffgapcx1.set("quickplane", "xz");
                wp_make_cuffgapcx1.set("unite", true);
                wp_make_cuffgapcx1.geom().create("r1", "Rectangle");
                wp_make_cuffgapcx1.geom().feature("r1").label("Cuff Gap Cross Section");
                wp_make_cuffgapcx1.geom().feature("r1").set("pos", new String[]{"R_in+((R_out-R_in)/2)", "Center"});
                wp_make_cuffgapcx1.geom().feature("r1").set("base", "center");
                wp_make_cuffgapcx1.geom().feature("r1").set("size", new String[]{"R_out-R_in", "L"});

                String revmcg1Label = "Make Cuff Gap 1";
                GeomFeature rev_make_cuffgap1 = model.geom(id).create(im.next("rev",revmcg1Label), "Revolve");
                rev_make_cuffgap1.label(revmcg1Label);
                rev_make_cuffgap1.set("contributeto", im.get("CUFF GAP"));
                rev_make_cuffgap1.set("angle1", "Theta");
                rev_make_cuffgap1.selection("input").named(im.get("CUFF GAP CROSS SECTION"));

                String difrcg1Label = "Remove Cuff Gap 1";
                GeomFeature dif_remove_cuffgap1 = model.geom(id).create(im.next("dif",difrcg1Label), "Difference");
                dif_remove_cuffgap1.label(difrcg1Label);
                dif_remove_cuffgap1.set("contributeto", im.get("CUFF wGAP PRE HOLES"));
                dif_remove_cuffgap1.selection("input").named(im.get("CUFF PRE GAP PRE HOLES"));
                dif_remove_cuffgap1.selection("input2").named(im.get("CUFF GAP"));

                String econmhs1Label = "Make Hole Shape 1";
                GeomFeature econ_makehole1 = model.geom(id).create(im.next("econ",econmhs1Label), "ECone");
                econ_makehole1.label(econmhs1Label);
                econ_makehole1.set("contributeto", im.get("HOLES"));
                econ_makehole1.set("pos", new String[]{"R_in-Buffer_hole/2", "0", "Center+Pitch_holecenter_holecenter/2"});
                econ_makehole1.set("axis", new int[]{1, 0, 0});
                econ_makehole1.set("semiaxes", new String[]{"D_hole/2", "D_hole/2"});
                econ_makehole1.set("h", "(R_out-R_in)+Buffer_hole");
                econ_makehole1.set("rat", "R_out/R_in");

                String rotphic1Label = "Position Hole in Cuff 1";
                GeomFeature rot_position_hole1 = model.geom(id).create(im.next("rot",rotphic1Label), "Rotate");
                rot_position_hole1.label(rotphic1Label);
                rot_position_hole1.set("rot", "(360*L_holecenter_cuffseam)/(pi*2*R_in)");
                rot_position_hole1.selection("input").named(im.get("HOLES"));

                String difmich1Label = "Make Inner Cuff Hole 1";
                GeomFeature dif_make_hole1 = model.geom(id).create(im.next("dif",difmich1Label), "Difference");
                dif_make_hole1.label(difmich1Label);
                dif_make_hole1.set("contributeto", im.get("CUFF FINAL"));
                dif_make_hole1.selection("input").named(im.get("CUFF wGAP PRE HOLES"));
                dif_make_hole1.selection("input2").named(im.get("HOLES"));

                String rotdcLabel = "Rotate to Default Conformation";
                GeomFeature rot_default_conformation = model.geom(id).create(im.next("rot",rotdcLabel), "Rotate");
                rot_default_conformation.label(rotdcLabel);
                rot_default_conformation.set("rot", "Rot_def");
                rot_default_conformation.selection("input").named(im.get("CUFF FINAL"));

                String endifLabel = "End";
                GeomFeature endif = model.geom(id).create(im.next("endif", endifLabel), "EndIf");
                endif.label(endifLabel);

                model.geom(id).run();
                break;
            case "RibbonContact_Primitive":

                mp.set("Thk_elec", "0.1 [mm]");
                mp.set("L_elec", "3 [mm]");
                mp.set("R_in", "1 [mm]");
                mp.set("Recess", "0.1 [mm]");
                mp.set("Center", "10 [mm]");
                mp.set("Theta_contact", "100 [deg]");
                mp.set("Rot_def", "0 [deg]");

                String[] cselRiCLabels = {
                        "CONTACT CROSS SECTION",
                        "RECESS CROSS SECTION",
                        "SRC",
                        "CONTACT FINAL",
                        "RECESS FINAL"
                };
                for (String cselRiCLabel: cselRiCLabels) {
                    model.geom(id).selection().create(im.next("csel", cselRiCLabel), "CumulativeSelection")
                            .label(cselRiCLabel);
                }

                String wpccxLabel = "Contact Cross Section";
                GeomFeature wp_contact_cx = model.geom(id).create(im.next("wp",wpccxLabel), "WorkPlane");
                wp_contact_cx.label(wpccxLabel);
                wp_contact_cx.set("contributeto",im.get("CONTACT CROSS SECTION"));
                wp_contact_cx.set("quickplane", "xz");
                wp_contact_cx.set("unite", true);
                wp_contact_cx.geom().create("r1", "Rectangle");
                wp_contact_cx.geom().feature("r1").label("Contact Cross Section");
                wp_contact_cx.geom().feature("r1")
                        .set("pos", new String[]{"R_in+Recess+Thk_elec/2", "Center"});
                wp_contact_cx.geom().feature("r1").set("base", "center");
                wp_contact_cx.geom().feature("r1").set("size", new String[]{"Thk_elec", "L_elec"});

                String revmcLabel = "Make Contact";
                GeomFeature rev_make_contact = model.geom(id).create(im.next("rev",revmcLabel), "Revolve");
                rev_make_contact.label("Make Contact");
                rev_make_contact.set("contributeto", im.get("CONTACT FINAL"));
                rev_make_contact.set("angle1", "Rot_def");
                rev_make_contact.set("angle2", "Rot_def+Theta_contact");
                rev_make_contact.selection("input").named(im.get("CONTACT CROSS SECTION"));

                String ifrecessLabel = "IF RECESS";
                GeomFeature if_recess = model.geom(id).create(im.next("if",ifrecessLabel), "If");
                if_recess.set("condition", "Recess>0");
                if_recess.label(ifrecessLabel);

                String wprcx1Label = "Recess Cross Section 1";
                GeomFeature wp_recess_cx1 = model.geom(id).create(im.next("wp",wprcx1Label), "WorkPlane");
                wp_recess_cx1.label(wprcx1Label);
                wp_recess_cx1.set("contributeto", im.get("RECESS CROSS SECTION"));
                wp_recess_cx1.set("quickplane", "xz");
                wp_recess_cx1.set("unite", true);

                im.next(im.get(wprcx1Label) + "_" + "csel", "Cumulative Selection 1");
                wp_recess_cx1.geom().selection().create(im.get("Cumulative Selection 1").split("_")[1], "CumulativeSelection");
                wp_recess_cx1.geom().selection("csel1").label("Cumulative Selection 1");

                im.next(im.get(wprcx1Label) + "_" + "csel", "RECESS CROSS SECTION");
                wp_recess_cx1.geom().selection().create(im.get("RECESS CROSS SECTION").split("_")[1], "CumulativeSelection");
                wp_recess_cx1.geom().selection("csel2").label("RECESS CROSS SECTION");

                wp_recess_cx1.geom().create("r1", "Rectangle");
                wp_recess_cx1.geom().feature("r1").label("Recess Cross Section");
                wp_recess_cx1.geom().feature("r1").set("contributeto", im.get("RECESS CROSS SECTION"));
                wp_recess_cx1.geom().feature("r1").set("pos", new String[]{"R_in+Recess/2", "Center"});
                wp_recess_cx1.geom().feature("r1").set("base", "center");
                wp_recess_cx1.geom().feature("r1").set("size", new String[]{"Recess", "L_elec"});

                String revmrLabel = "Make Recess";
                GeomFeature rev_make_racess = model.geom(id).create(im.next("rev",revmrLabel), "Revolve");
                rev_make_racess.label(revmrLabel);
                rev_make_racess.set("contributeto", im.get("RECESS FINAL"));
                rev_make_racess.set("angle1", "Rot_def");
                rev_make_racess.set("angle2", "Rot_def+Theta_contact");
                rev_make_racess.selection("input").named(im.get("RECESS CROSS SECTION"));

                endifLabel = "EndIf";
                model.geom(id).create(im.next("endif"), endifLabel).label(endifLabel);

                String srcLabel = "Src";
                GeomFeature src = model.geom(id).create(im.next("pt",srcLabel), "Point");
                src.label(srcLabel);
                src.set("contributeto", im.get("SRC"));
                src.set("p", new String[]{"(R_in+Recess+Thk_elec/2)*cos(Rot_def+Theta_contact/2)", "(R_in+Recess+Thk_elec/2)*sin(Rot_def+Theta_contact/2)", "Center"});

                model.geom(id).run();
                break;
            case "WireContact_Primitive":
                model.geom(id).inputParam().set("R_conductor", "r_conductor_P");
                model.geom(id).inputParam().set("R_in", "R_in_P");
                model.geom(id).inputParam().set("Center", "Center_P");
                model.geom(id).inputParam().set("Pitch", "Pitch_P");
                model.geom(id).inputParam().set("Sep_conductor", "sep_conductor_P");
                model.geom(id).inputParam().set("Theta_conductor", "theta_conductor_P");

                String[] cselWCLabels = {
                        "CONTACT CROSS SECTION",
                        "CONTACT FINAL",
                        "SRC"
                };
                for (String cselWCLabel: cselWCLabels) {
                    model.geom(id).selection().create(im.next("csel", cselWCLabel), "CumulativeSelection")
                            .label(cselWCLabel);
                }

                String contactxsLabel = "Contact Cross Section";
                GeomFeature contact_xs = model.geom(id).create(im.next("wp",contactxsLabel), "WorkPlane");
                contact_xs.set("contributeto", im.get("CONTACT CROSS SECTION"));
                contact_xs.label(contactxsLabel);
                contact_xs.set("quickplane", "zx");
                contact_xs.set("unite", true);
                contact_xs.geom().selection().create(im.get("CONTACT CROSS SECTION"), "CumulativeSelection");
                contact_xs.geom().selection(im.get("CONTACT CROSS SECTION")).label("CONTACT CROSS SECTION");
                contact_xs.geom().create("c1", "Circle");
                contact_xs.geom().feature("c1").label("Contact Cross Section");
                contact_xs.geom().feature("c1").set("contributeto", im.get("CONTACT CROSS SECTION"));
                contact_xs.geom().feature("c1").set("pos", new String[]{"Center", "R_in-R_conductor-Sep_conductor"});
                contact_xs.geom().feature("c1").set("r", "R_conductor");

                String mcLabel = "Make Contact";
                GeomFeature contact = model.geom(id).create(im.next("rev",mcLabel), "Revolve");
                contact.label(mcLabel);
                contact.set("contributeto", im.get("CONTACT FINAL"));
                contact.set("angle2", "Theta_conductor");
                contact.set("axis", new int[]{1, 0});
                contact.selection("input").named(im.get("CONTACT CROSS SECTION"));

                String sourceLabel = "Src";
                GeomFeature source = model.geom(id).create(im.next("pt",sourceLabel), "Point");
                source.label(sourceLabel);
                source.set("contributeto", im.get("SRC"));
                source.set("p", new String[]{"(R_in-R_conductor-Sep_conductor)*cos(Theta_conductor/2)", "(R_in-R_conductor-Sep_conductor)*sin(Theta_conductor/2)", "Center"});
                model.geom(id).run();
                break;
            case "CircleContact_Primitive":
                model.geom(id).inputParam().set("Recess", "Recess_ITC");
                model.geom(id).inputParam().set("Rotation_angle", "0 [deg]");
                model.geom(id).inputParam().set("Center", "Center_IT");
                model.geom(id).inputParam().set("Round_def", "Round_def_ITC");
                model.geom(id).inputParam().set("R_in", "R_in_ITI");
                model.geom(id).inputParam().set("Contact_depth", "Contact_depth_ITC");
                model.geom(id).inputParam().set("Overshoot", "Overshoot_ITC");
                model.geom(id).inputParam().set("A_ellipse_contact", "a_ellipse_contact_ITC");
                model.geom(id).inputParam().set("Diam_contact", "diam_contact_ITC");
                model.geom(id).inputParam().set("L", "L_IT");

                String[] cselCCLabels = {
                        "CONTACT CUTTER IN",
                        "PRE CUT CONTACT",
                        "RECESS FINAL",
                        "RECESS OVERSHOOT",
                        "SRC",
                        "PLANE FOR CONTACT",
                        "CONTACT FINAL",
                        "CONTACT CUTTER OUT",
                        "BASE CONTACT PLANE (PRE ROTATION)",
                        "PLANE FOR RECESS",
                        "PRE CUT RECESS",
                        "RECESS CUTTER IN",
                        "RECESS CUTTER OUT"
                };
                for (String cselCCLabel: cselCCLabels) {
                    model.geom(id).selection().create(im.next("csel", cselCCLabel), "CumulativeSelection")
                            .label(cselCCLabel);
                }

                String bpprLabel = "Base Plane (Pre Rrotation)";
                GeomFeature baseplane_prerot = model.geom(id).create(im.next("wp", bpprLabel), "WorkPlane");
                baseplane_prerot.label(bpprLabel);
                baseplane_prerot.set("contributeto", im.get("BASE PLANE (PRE ROTATION)"));
                baseplane_prerot.set("quickplane", "yz");
                baseplane_prerot.set("unite", true);

                String ifrecessCCLabel = "If Recess";
                GeomFeature ifrecessCC = model.geom(id).create(im.next("if",ifrecessCCLabel), "If");
                ifrecessCC.label(ifrecessCCLabel);
                ifrecessCC.set("condition", "Recess>0");

                String rprLabel = "Rotated Plane for Recess";
                GeomFeature rpr = model.geom(id).create(im.next("wp",rprLabel), "WorkPlane");
                rpr.label(rprLabel);
                rpr.set("contributeto", im.get("PLANE FOR RECESS"));
                rpr.set("planetype", "transformed");
                rpr.set("workplane", im.get("Rotated Plane for Recess"));
                rpr.set("transaxis", new int[]{0, 1, 0});
                rpr.set("transrot", "Rotation_angle");
                rpr.set("unite", true);

                String cosLabel = "CONTACT OUTLINE SHAPE";
                rpr.geom().selection().create(im.next("csel",cosLabel), "CumulativeSelection");
                rpr.geom().selection(im.get(cosLabel)).label(cosLabel);

                String ifcsicLabel = "If Contact Surface is Circle";
                GeomFeature ifcsic = rpr.geom().create(im.next("if",ifcsicLabel), "If");
                ifcsic.label("If Contact Surface is Circle");
                ifcsic.set("condition", "Round_def==1");

                String coLabel = "Contact Outline";
                GeomFeature co = rpr.geom().create(im.next("e",coLabel), "Ellipse");
                co.label("Contact Outline");
                co.set("contributeto", im.get("CONTACT OUTLINE SHAPE"));
                co.set("pos", new String[]{"0", "Center"});
                co.set("semiaxes", new String[]{"A_ellipse_contact", "Diam_contact/2"});

                String elifcocLabel = "Else If Contact Outline is Circle";
                GeomFeature elifcoc = rpr.geom().create(im.next("elseif",elifcocLabel), "ElseIf");
                elifcoc.label("Else If Contact Outline is Circle");
                elifcoc.set("condition", "Round_def==2");

                String co1Label = "Contact Outline 1";
                GeomFeature co1 = rpr.geom().create(im.next("e",co1Label), "Ellipse");
                co1.label(co1Label);
                co1.set("contributeto", im.get("CONTACT OUTLINE SHAPE"));
                co1.set("pos", new String[]{"0", "Center"});
                co1.set("semiaxes", new String[]{"Diam_contact/2", "Diam_contact/2"});
                rpr.geom().create(im.next("endif"), "EndIf");

                String mpcrdLabel = "Make Pre Cut Recess Domains";
                GeomFeature mpcrd = model.geom(id).create(im.next("ext",mpcrdLabel), "Extrude");
                mpcrd.label(mpcrdLabel);
                mpcrd.set("contributeto", im.get("PRE CUT RECESS"));
                mpcrd.setIndex("distance", "R_in+Recess+Overshoot", 0);
                mpcrd.selection("input").named(im.get("PLANE FOR RECESS"));

                String rciLabel = "Recess Cut In";
                GeomFeature rci = model.geom(id).create(im.next("cyl",rciLabel), "Cylinder");
                rci.label(rciLabel);
                rci.set("contributeto", im.get("RECESS CUTTER IN"));
                rci.set("pos", new String[]{"0", "0", "Center-L/2"});
                rci.set("r", "R_in");
                rci.set("h", "L");

                String rcoLabel = "Recess Cut Out";
                GeomFeature rco = model.geom(id).create(im.next("cyl",rcoLabel), "Cylinder");
                rco.label(rcoLabel);
                rco.set("contributeto", im.get("RECESS CUTTER OUT"));
                rco.set("pos", new String[]{"0", "0", "Center-L/2"});
                rco.set("r", "R_in+Recess");
                rco.set("h", "L");

                String erciLabel = "Execute Recess Cut In";
                GeomFeature erci = model.geom(id).create(im.next("dif",erciLabel), "Difference");
                erci.label(erciLabel);
                erci.set("contributeto", im.get("RECESS FINAL"));
                erci.selection("input").named(im.get("PRE CUT RECESS"));
                erci.selection("input2").named(im.get("RECESS CUTTER IN"));

                String pordLabel = "Partition Outer Recess Domain";
                GeomFeature pord = model.geom(id).create(im.next("pard", pordLabel), "PartitionDomains");
                pord.label(pordLabel);
                pord.set("contributeto", im.get("RECESS FINAL"));
                pord.set("partitionwith", "objects");
                pord.set("keepobject", false);
                pord.selection("domain").named(im.get("PRE CUT RECESS"));
                pord.selection("object").named(im.get("RECESS CUTTER OUT"));

                String soLabel = "Select Overshoot";
                GeomFeature so = model.geom(id).create(im.next("ballsel",soLabel), "BallSelection");
                so.label(soLabel);
                so.set("posx", "(R_in+Recess+Overshoot/2)*cos(Rotation_angle)");
                so.set("posy", "(R_in+Recess+Overshoot/2)*sin(Rotation_angle)");
                so.set("posz", "Center");
                so.set("r", 1);
                so.set("contributeto", im.get("RECESS OVERSHOOT"));

                String droLabel = "Delete Recess Overshoot";
                GeomFeature dro = model.geom(id).create(im.next("del",droLabel), "Delete");
                dro.label(droLabel);
                dro.selection("input").init(3);
                dro.selection("input").named(im.get("RECESS OVERSHOOT"));

                String endifrecessLabel = "EndIf";
                model.geom(id).create(im.next("endif"), endifrecessLabel);

                String rpcLabel = "Rotated Plane for Contact";
                GeomFeature rpc = model.geom(id).create(im.next("wp",rpcLabel), "WorkPlane");
                rpc.label(rpcLabel);
                rpc.set("contributeto", im.get("PLANE FOR CONTACT"));
                rpc.set("planetype", "transformed");
                rpc.set("workplane", im.get("Base Plane (Pre Rrotation)"));
                rpc.set("transaxis", new int[]{0, 1, 0});
                rpc.set("transrot", "Rotation_angle");
                rpc.set("unite", true);

                String coscLabel = "CONTACT OUTLINE SHAPE";

                rpc.geom().selection().create(im.next("csel",coscLabel), "CumulativeSelection");
                rpc.geom().selection(im.get(coscLabel)).label(coscLabel);

                String ifcsiccLabel = "If Contact Surface is Circle";
                GeomFeature icsicc = rpc.geom().create(im.next("if",ifcsiccLabel), "If");
                icsicc.label(ifcsiccLabel);
                icsicc.set("condition", "Round_def==1");

                String cocLabel = "Contact Outline";
                GeomFeature coc = rpc.geom().create(im.next("e",cocLabel), "Ellipse");
                coc.label(cocLabel);
                coc.set("contributeto", "csel1");
                coc.set("pos", new String[]{"0", "Center"});
                coc.set("semiaxes", new String[]{"A_ellipse_contact", "Diam_contact/2"});

                String elifcoccLabel = "Else If Contact Outline is Circle";
                GeomFeature elifcocc = rpc.geom().create(im.next("elseif",elifcoccLabel), "ElseIf");
                elifcoc.label("Else If Contact Outline is Circle");
                elifcocc.set("condition", "Round_def==2");

                String co1cLabel = "Contact Outline 1";
                GeomFeature co1c = rpc.geom().create(im.next("e",co1cLabel), "Ellipse");
                co1c.label("Contact Outline 1");
                co1c.set("contributeto", im.get("CONTACT OUTLINE SHAPE"));
                co1c.set("pos", new String[]{"0", "Center"});
                co1c.set("semiaxes", new String[]{"Diam_contact/2", "Diam_contact/2"});
                rpc.geom().create(im.next("endif"), "EndIf");

                String mpccdLabel = "Make Pre Cut Contact Domains";
                GeomFeature mpccd = model.geom(id).create(im.next("ext",mpccdLabel), "Extrude");
                mpccd.label(mpccdLabel);
                mpccd.set("contributeto", im.get("PRE CUT CONTACT"));
                mpccd.setIndex("distance", "R_in+Recess+Contact_depth+Overshoot", 0);
                mpccd.selection("input").named(im.get("PLANE FOR CONTACT"));

                String cciLabel = "Contact Cut In";
                GeomFeature cci = model.geom(id).create(im.next("cyl",cciLabel), "Cylinder");
                cci.label(cciLabel);
                cci.set("contributeto", im.get("CONTACT CUTTER IN"));
                cci.set("pos", new String[]{"0", "0", "Center-L/2"});
                cci.set("r", "R_in+Recess");
                cci.set("h", "L");

                String ccoLabel = "Contact Cut Out";
                GeomFeature cco = model.geom(id).create(im.next("cyl",ccoLabel), "Cylinder");
                cco.label(ccoLabel);
                cco.set("contributeto", im.get("CONTACT CUTTER OUT"));
                cco.set("pos", new String[]{"0", "0", "Center-L/2"});
                cco.set("r", "R_in+Recess+Contact_depth");
                cco.set("h", "L");

                String ecciLabel = "Execute Contact Cut In";
                GeomFeature ecci = model.geom(id).create(im.next("dif",ecciLabel), "Difference");
                ecci.label(ecciLabel);
                ecci.set("contributeto", im.get("CONTACT FINAL"));
                ecci.selection("input").named("PRE CUT CONTACT");
                ecci.selection("input2").named(im.get("CONTACT CUTTER IN"));

                String pocdLabel = "Partition Outer Contact Domain";
                GeomFeature pocd = model.geom(id).create(im.next("pard",pocdLabel), "PartitionDomains");
                pocd.label(pocdLabel);
                pocd.set("contributeto", im.get("CONTACT FINAL"));
                pocd.set("partitionwith", "objects");
                pocd.set("keepobject", false);
                pocd.selection("domain").named(im.get("PRE CUT CONTACT"));
                pocd.selection("object").named(im.get("CONTACT CUTTER OUT"));

                String so1Label = "Select Overshoot 1";
                GeomFeature so1 = model.geom(id).create(im.next("ballsel", so1Label), "BallSelection");
                so1.label(so1Label);
                so1.set("posx", "(R_in+Recess+Contact_depth+Overshoot/2)*cos(Rotation_angle)");
                so1.set("posy", "(R_in+Recess+Contact_depth+Overshoot/2)*sin(Rotation_angle)");
                so1.set("posz", "Center");
                so1.set("r", 1);
                so1.set("contributeto", im.get("RECESS OVERSHOOT"));

                String dro1Label = "Delete Recess Overshoot 1";
                GeomFeature dro1 = model.geom(id).create(im.next("del",dro1Label), "Delete");
                dro1.label(dro1Label);
                dro1.selection("input").init(3);
                dro1.selection("input").named(im.get("RECESS OVERSHOOT"));

                String srccLabel = "Src";
                GeomFeature srcc = model.geom(id).create(im.next("pt",srccLabel), "Point");
                srcc.label(srccLabel);
                srcc.set("contributeto", mw.get("SRC"));
                srcc.set("p", new String[]{"(R_in+Recess+Contact_depth/2)*cos(Rotation_angle)", "(R_in+Recess+Contact_depth/2)*sin(Rotation_angle)", "Center"});

                model.geom(id).run();
                break;
            case "HelicalCuffnContact_Primitive":
                model.geom(id).inputParam().set("Center", "Center_LN");

                String[] cselHCCLabels = {
                        "PC1",
                        "Cuffp1",
                        "SEL END P1",
                        "PC2",
                        "SRC",
                        "Cuffp2",
                        "Conductorp2",
                        "SEL END P2",
                        "Cuffp3",
                        "PC3"
                };
                for (String cselHCCLabel: cselHCCLabels) {
                    model.geom(id).selection().create(im.next("csel", cselHCCLabel), "CumulativeSelection")
                            .label(cselHCCLabel);
                }

                model.geom(id).create(mw.next("wp","Helical Insulator Cross Section Part 1"), "WorkPlane");
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).label("Helical Insulator Cross Section Part 1");
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).set("quickplane", "xz");
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).set("unite", true);
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).geom().selection().create("csel1", "CumulativeSelection");               // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).geom().selection("csel1").label("HELICAL INSULATOR CROSS SECTION");          // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).geom().selection().create("csel2", "CumulativeSelection");               // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).geom().selection("csel2").label("HELICAL INSULATOR CROSS SECTION P1");       // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).geom().create("r1", "Rectangle");                                        // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).geom().feature("r1").label("Helical Insulator Cross Section Part 1");        // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).geom().feature("r1").set("contributeto", "csel2");                           // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).geom().feature("r1")                                                         // TODO
                        .set("pos", new String[]{"r_cuff_in_LN+(thk_cuff_LN/2)", "Center-(L_cuff_LN/2)"});
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).geom().feature("r1").set("base", "center");                                  // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 1")).geom().feature("r1").set("size", new String[]{"thk_cuff_LN", "w_cuff_LN"});  // TODO

                model.geom(id).create(mw.next("pc","Parametric Curve Part 1"), "ParametricCurve");
                model.geom(id).feature(mw.get("Parametric Curve Part 1")).label("Parametric Curve Part 1");
                model.geom(id).feature(mw.get("Parametric Curve Part 1")).set("contributeto", mw.get("PC1"));
                model.geom(id).feature(mw.get("Parametric Curve Part 1")).set("parmax", "rev_cuff_LN*(0.75/2.5)");
                model.geom(id).feature(mw.get("Parametric Curve Part 1"))
                        .set("coord", new String[]{"cos(2*pi*s)*((thk_cuff_LN/2)+r_cuff_in_LN)", "sin(2*pi*s)*((thk_cuff_LN/2)+r_cuff_in_LN)", "Center+(L_cuff_LN)*(s/rev_cuff_LN)-(L_cuff_LN/2)"});

                model.geom(id).create(mw.next("swe","Make Cuff Part 1"), "Sweep");
                model.geom(id).feature(mw.get("Make Cuff Part 1")).label("Make Cuff Part 1");
                model.geom(id).feature(mw.get("Make Cuff Part 1")).set("contributeto", mw.get("Cuffp1"));
                model.geom(id).feature(mw.get("Make Cuff Part 1")).set("crossfaces", true);
                model.geom(id).feature(mw.get("Make Cuff Part 1")).set("keep", false);
                model.geom(id).feature(mw.get("Make Cuff Part 1")).set("includefinal", false);
                model.geom(id).feature(mw.get("Make Cuff Part 1")).set("twistcomp", false);
                model.geom(id).feature(mw.get("Make Cuff Part 1")).selection("face").named("wp1_csel2");  // TODO
                model.geom(id).feature(mw.get("Make Cuff Part 1")).selection("edge").named(mw.get("PC1")); // TODO
                model.geom(id).feature(mw.get("Make Cuff Part 1")).selection("diredge").set("pc1(1)", 1); // TODO

                model.geom(id).create(mw.next("ballsel", "Select End Face Part 1"), "BallSelection");
                model.geom(id).feature(mw.get("Select End Face Part 1")).set("entitydim", 2);
                model.geom(id).feature(mw.get("Select End Face Part 1")).label("Select End Face Part 1");
                model.geom(id).feature(mw.get("Select End Face Part 1")).set("posx", "cos(2*pi*rev_cuff_LN*((0.75)/2.5))*((thk_cuff_LN/2)+r_cuff_in_LN)");
                model.geom(id).feature(mw.get("Select End Face Part 1")).set("posy", "sin(2*pi*rev_cuff_LN*((0.75)/2.5))*((thk_cuff_LN/2)+r_cuff_in_LN)");
                model.geom(id).feature(mw.get("Select End Face Part 1"))
                        .set("posz", "Center+(L_cuff_LN)*(rev_cuff_LN*((0.75)/2.5)/rev_cuff_LN)-(L_cuff_LN/2)");
                model.geom(id).feature(mw.get("Select End Face Part 1")).set("r", 1);
                model.geom(id).feature(mw.get("Select End Face Part 1")).set("contributeto", mw.get("SEL END P1"));

                model.geom(id).create(mw.next("wp","Helical Insulator Cross Section Part 2"), "WorkPlane");
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).label("Helical Insulator Cross Section Part 2");
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).set("planetype", "faceparallel");
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).set("unite", true);
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).selection("face").named("csel3");                                        // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).geom().selection().create("csel1", "CumulativeSelection");           // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).geom().selection("csel1").label("HELICAL INSULATOR CROSS SECTION P2");   // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).geom().selection().create("csel2", "CumulativeSelection");           // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).geom().selection("csel2").label("HELICAL CONDUCTOR CROSS SECTION P2");   // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).geom().create("r1", "Rectangle"); // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).geom().feature("r1").label("Helical Insulator Cross Section Part 2"); // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).geom().feature("r1").set("contributeto", "csel1");                       // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).geom().feature("r1").set("base", "center"); // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 2")).geom().feature("r1").set("size", new String[]{"thk_cuff_LN", "w_cuff_LN"}); // TODO

                model.geom(id).create(mw.next("wp","Helical Conductor Cross Section Part 2"), "WorkPlane");
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).label("Helical Conductor Cross Section Part 2");
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).set("planetype", "faceparallel");
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).set("unite", true);
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).selection("face").named("csel3");                                      // TODO
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).geom().selection().create("csel1", "CumulativeSelection");         // TODO
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).geom().selection("csel1").label("HELICAL INSULATOR CROSS SECTION P2"); // TODO
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).geom().selection().create("csel2", "CumulativeSelection");         // TODO
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).geom().selection("csel2").label("HELICAL CONDUCTOR CROSS SECTION P2"); // TODO
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).geom().create("r2", "Rectangle");
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).geom().feature("r2").label("Helical Conductor Cross Section Part 2");  // TODO
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).geom().feature("r2").set("contributeto", "csel2");                     // TODO
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).geom().feature("r2").set("pos", new String[]{"(thk_elec_LN-thk_cuff_LN)/2", "0"});
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).geom().feature("r2").set("base", "center");
                model.geom(id).feature(mw.next("wp","Helical Conductor Cross Section Part 2")).geom().feature("r2").set("size", new String[]{"thk_elec_LN", "w_elec_LN"});

                model.geom(id).create(mw.next("pc","Parametric Curve Part 2"), "ParametricCurve");
                model.geom(id).feature(mw.get("Parametric Curve Part 2")).label("Parametric Curve Part 2");
                model.geom(id).feature(mw.get("Parametric Curve Part 2")).set("contributeto", "csel4");
                model.geom(id).feature(mw.get("Parametric Curve Part 2")).set("parmin", "rev_cuff_LN*(0.75/2.5)");
                model.geom(id).feature(mw.get("Parametric Curve Part 2")).set("parmax", "rev_cuff_LN*((0.75+1)/2.5)");
                model.geom(id).feature(mw.get("Parametric Curve Part 2"))
                        .set("coord", new String[]{"cos(2*pi*s)*((thk_cuff_LN/2)+r_cuff_in_LN)", "sin(2*pi*s)*((thk_cuff_LN/2)+r_cuff_in_LN)", "Center+(L_cuff_LN)*(s/rev_cuff_LN)-(L_cuff_LN/2)"});

                model.geom(id).create(mw.next("swe","Make Cuff Part 2"), "Sweep");
                model.geom(id).feature(mw.get("Make Cuff Part 2")).label("Make Cuff Part 2");
                model.geom(id).feature(mw.get("Make Cuff Part 2")).set("contributeto", mw.get("Cuffp2"));
                model.geom(id).feature(mw.get("Make Cuff Part 2")).set("crossfaces", true);
                model.geom(id).feature(mw.get("Make Cuff Part 2")).set("includefinal", false);
                model.geom(id).feature(mw.get("Make Cuff Part 2")).set("twistcomp", false);
                model.geom(id).feature(mw.get("Make Cuff Part 2")).selection("face").named("wp2_csel1"); //TODO
                model.geom(id).feature(mw.get("Make Cuff Part 2")).selection("edge").named("csel4"); //TODO
                model.geom(id).feature(mw.get("Make Cuff Part 2")).selection("diredge").set("pc2(1)", 1); //TODO

                model.geom(id).create(mw.next("swe","Make Conductor Part 2"), "Sweep");
                model.geom(id).feature(mw.get("Make Conductor Part 2")).label("Make Conductor Part 2");
                model.geom(id).feature(mw.get("Make Conductor Part 2")).set("contributeto", mw.get("Conductorp2"));
                model.geom(id).feature(mw.get("Make Conductor Part 2")).set("crossfaces", true);
                model.geom(id).feature(mw.get("Make Conductor Part 2")).set("keep", false);
                model.geom(id).feature(mw.get("Make Conductor Part 2")).set("includefinal", false);
                model.geom(id).feature(mw.get("Make Conductor Part 2")).set("twistcomp", false);
                model.geom(id).feature(mw.get("Make Conductor Part 2")).selection("face").named("wp3_csel2"); //TODO
                model.geom(id).feature(mw.get("Make Conductor Part 2")).selection("edge").named(mw.get("PC2")); //TODO
                model.geom(id).feature(mw.get("Make Conductor Part 2")).selection("diredge").set("pc2(1)", 1); //TODO

                model.geom(id).create(mw.next("ballsel","Select End Face Part 2"), "BallSelection");
                model.geom(id).feature(mw.get("Select End Face Part 2")).set("entitydim", 2);
                model.geom(id).feature(mw.get("Select End Face Part 2")).label("Select End Face Part 2");
                model.geom(id).feature(mw.get("Select End Face Part 2"))
                        .set("posx", "cos(2*pi*rev_cuff_LN*((0.75+1)/2.5))*((thk_cuff_LN/2)+r_cuff_in_LN)");
                model.geom(id).feature(mw.get("Select End Face Part 2"))
                        .set("posy", "sin(2*pi*rev_cuff_LN*((0.75+1)/2.5))*((thk_cuff_LN/2)+r_cuff_in_LN)");
                model.geom(id).feature(mw.get("Select End Face Part 2"))
                        .set("posz", "Center+(L_cuff_LN)*(rev_cuff_LN*((0.75+1)/2.5)/rev_cuff_LN)-(L_cuff_LN/2)");
                model.geom(id).feature(mw.get("Select End Face Part 2")).set("r", 1);
                model.geom(id).feature(mw.get("Select End Face Part 2")).set("contributeto", mw.get("SEL END P2"));

                model.geom(id).create(mw.next("wp","Helical Insulator Cross Section Part 3"), "WorkPlane");
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 3")).label("Helical Insulator Cross Section Part 3");
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 3")).set("planetype", "faceparallel");
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 3")).set("unite", true);
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 3")).selection("face").named(mw.get("SEL END P2"));
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 3")).geom().selection().create("csel1", "CumulativeSelection"); //TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 3")).geom().selection("csel1").label("HELICAL INSULATOR CROSS SECTION P3"); //TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 3")).geom().create("r1", "Rectangle"); // TODO
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 3")).geom().feature("r1").label("Helical Insulator Cross Section Part 3");
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 3")).geom().feature("r1").set("contributeto", "csel1"); //TODO - might not be necessary? maybe on the ones that are like wp1_csel1 need this actually
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 3")).geom().feature("r1").set("base", "center");
                model.geom(id).feature(mw.get("Helical Insulator Cross Section Part 3")).geom().feature("r1").set("size", new String[]{"thk_cuff_LN", "w_cuff_LN"});

                model.geom(id).create(mw.next("pc","Parametric Curve Part 3"), "ParametricCurve");
                model.geom(id).feature(mw.get("Parametric Curve Part 3")).label("Parametric Curve Part 3");
                model.geom(id).feature(mw.get("Parametric Curve Part 3")).set("contributeto", mw.get("PC3"));
                model.geom(id).feature(mw.get("Parametric Curve Part 3")).set("parmin", "rev_cuff_LN*((0.75+1)/2.5)");
                model.geom(id).feature(mw.get("Parametric Curve Part 3")).set("parmax", "rev_cuff_LN");
                model.geom(id).feature(mw.get("Parametric Curve Part 3"))
                        .set("coord", new String[]{"cos(2*pi*s)*((thk_cuff_LN/2)+r_cuff_in_LN)", "sin(2*pi*s)*((thk_cuff_LN/2)+r_cuff_in_LN)", "Center+(L_cuff_LN)*(s/rev_cuff_LN)-(L_cuff_LN/2)"});

                model.geom(id).create(mw.next("swe","Make Cuff Part 3"), "Sweep");
                model.geom(id).feature(mw.get("Make Cuff Part 3")).label("Make Cuff Part 3");
                model.geom(id).feature(mw.get("Make Cuff Part 3")).set("contributeto", mw.get("Cuffp3"));
                model.geom(id).feature(mw.get("Make Cuff Part 3")).selection("face").named("wp4_csel1"); //TODO
                model.geom(id).feature(mw.get("Make Cuff Part 3")).selection("edge").named(mw.get("PC3"));
                model.geom(id).feature(mw.get("Make Cuff Part 3")).set("keep", false);
                model.geom(id).feature(mw.get("Make Cuff Part 3")).set("twistcomp", false);

                model.geom(id).create(mw.next("pt","SRC"), "Point");
                model.geom(id).feature(mw.get("SRC")).label("src");
                model.geom(id).feature(mw.get("SRC")).set("contributeto", mw.get("SRC"));
                model.geom(id).feature(mw.get("SRC"))
                        .set("p", new String[]{"cos(2*pi*rev_cuff_LN*(1.25/2.5))*((thk_elec_LN/2)+r_cuff_in_LN)", "sin(2*pi*rev_cuff_LN*(1.25/2.5))*((thk_elec_LN/2)+r_cuff_in_LN)", "Center"});
                model.geom(id).run();
                break;
            case "RectangleContact_Primitive":
                model.geom(id).inputParam().set("r_inner_contact", "r_cuff_in_Pitt+recess_Pitt");
                model.geom(id).inputParam().set("r_outer_contact", "r_cuff_in_Pitt+recess_Pitt+thk_contact_Pitt");
                model.geom(id).inputParam().set("z_center", "0 [mm]");
                model.geom(id).inputParam().set("rotation_angle", "0 [deg]");

                String[] cselReCLabels = {
                        "OUTER CONTACT CUTTER",
                        "SEL INNER EXCESS CONTACT",
                        "INNER CONTACT CUTTER",
                        "SEL OUTER EXCESS RECESS",
                        "SEL INNER EXCESS RECESS",
                        "OUTER CUTTER",
                        "FINAL RECESS",
                        "RECESS CROSS SECTION",
                        "OUTER RECESS CUTTER",
                        "RECESS PRE CUTS",
                        "INNER RECESS CUTTER",
                        "FINAL CONTACT",
                        "SEL OUTER EXCESS CONTACT",
                        "SEL OUTER EXCESS",
                        "SEL INNER EXCESS",
                        "BASE CONTACT PLANE (PRE ROTATION)",
                        "SRC",
                        "CONTACT PRE CUTS",
                        "CONTACT CROSS SECTION",
                        "INNER CUFF CUTTER",
                        "OUTER CUFF CUTTER",
                        "FINAL",
                        "INNER CUTTER"
                };
                for (String cselReCLabel: cselReCLabels) {
                    model.geom(id).selection().create(im.next("csel", cselReCLabel), "CumulativeSelection")
                            .label(cselReCLabel);
                }

                model.geom(id).create(mw.next("wp","base plane (pre rotation)"), "WorkPlane");
                model.geom(id).feature(mw.get("base plane (pre rotation)")).label("base plane (pre rotation)");
                model.geom(id).feature(mw.get("base plane (pre rotation)")).set("contributeto", mw.get("BASE CONTACT PLANE (PRE ROTATION)"));
                model.geom(id).feature(mw.get("base plane (pre rotation)")).set("quickplane", "yz");
                model.geom(id).feature(mw.get("base plane (pre rotation)")).set("unite", true);

                model.geom(id).create(mw.next("wp","Contact Cross Section"), "WorkPlane");
                model.geom(id).feature(mw.get("Contact Cross Section")).label("Contact Cross Section");
                model.geom(id).feature(mw.get("Contact Cross Section")).set("contributeto", mw.get("CONTACT CROSS SECTION"));
                model.geom(id).feature(mw.get("Contact Cross Section")).set("planetype", "transformed");
                model.geom(id).feature(mw.get("Contact Cross Section")).set("workplane", mw.get("Contact Cross Section"));
                model.geom(id).feature(mw.get("Contact Cross Section")).set("transaxis", new int[]{0, 1, 0});
                model.geom(id).feature(mw.get("Contact Cross Section")).set("transrot", "rotation_angle");
                model.geom(id).feature(mw.get("Contact Cross Section")).set("unite", true);
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().selection().create("csel1", "CumulativeSelection"); //TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().selection("csel1").label("CONTACT PRE FILLET"); //TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().selection().create("csel2", "CumulativeSelection"); //TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().selection("csel2").label("CONTACT FILLETED"); //TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().create("r1", "Rectangle"); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("r1").label("Contact Pre Fillet Corners"); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("r1").set("contributeto", "csel1"); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("r1").set("pos", new int[]{0, 0}); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("r1").set("base", "center"); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("r1").set("size", new String[]{"w_contact_Pitt", "z_contact_Pitt"}); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().create("fil1", "Fillet"); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("fil1").label("Fillet Corners"); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("fil1").set("contributeto", "csel2"); //TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("fil1").set("radius", "fillet_contact_Pitt"); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("fil1").selection("point").named("csel1"); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().create("sca1", "Scale"); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("sca1").set("type", "anisotropic"); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("sca1")// TODO
                        .set("factor", new String[]{"1", "scale_morph_w_contact_Pitt"});
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("sca1").selection("input").named("csel2"); //TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().create("mov1", "Move"); // TODO
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("mov1").set("disply", "z_center");
                model.geom(id).feature(mw.get("Contact Cross Section")).geom().feature("mov1").selection("input").named("csel2"); //TODO

                model.geom(id).create(mw.next("ext","Make Contact Pre Cuts"), "Extrude");
                model.geom(id).feature(mw.get("Make Contact Pre Cuts")).label("Make Contact Pre Cuts");
                model.geom(id).feature(mw.get("Make Contact Pre Cuts")).set("contributeto", mw.get("CONTACT PRE CUTS"));
                model.geom(id).feature(mw.get("Make Contact Pre Cuts")).setIndex("distance", "2*r_cuff_in_Pitt", 0);
                model.geom(id).feature(mw.get("Make Contact Pre Cuts")).selection("input").named(mw.get("CONTACT CROSS SECTION"));

                model.geom(id).create(mw.next("cyl","Inner Contact Cutter"), "Cylinder");
                model.geom(id).feature(mw.get("Inner Contact Cutter")).label("Inner Contact Cutter");
                model.geom(id).feature(mw.get("Inner Contact Cutter")).set("contributeto", mw.get("INNER CONTACT CUTTER"));
                model.geom(id).feature(mw.get("Inner Contact Cutter")).set("pos", new String[]{"0", "0", "-L_cuff_Pitt/2+z_center"});
                model.geom(id).feature(mw.get("Inner Contact Cutter")).set("r", "r_inner_contact");
                model.geom(id).feature(mw.get("Inner Contact Cutter")).set("h", "L_cuff_Pitt");

                model.geom(id).create(mw.next("cyl","Outer Contact Cutter"), "Cylinder");
                model.geom(id).feature(mw.get("Outer Contact Cutter")).label("Outer Contact Cutter");
                model.geom(id).feature(mw.get("Outer Contact Cutter")).set("contributeto", mw.get("OUTER CONTACT CUTTER"));
                model.geom(id).feature(mw.get("Outer Contact Cutter")).set("pos", new String[]{"0", "0", "-L_cuff_Pitt/2+z_center"});
                model.geom(id).feature(mw.get("Outer Contact Cutter")).set("r", "r_outer_contact");
                model.geom(id).feature(mw.get("Outer Contact Cutter")).set("h", "L_cuff_Pitt");

                model.geom(id).create(mw.next("par","Cut Outer Excess"), "Partition");
                model.geom(id).feature(mw.get("Cut Outer Excess")).label("Cut Outer Excess"); // added this
                model.geom(id).feature(mw.get("Cut Outer Excess")).set("contributeto", mw.get("FINAL CONTACT"));
                model.geom(id).feature(mw.get("Cut Outer Excess")).selection("input").named(mw.get("CONTACT PRE CUTS"));
                model.geom(id).feature(mw.get("Cut Outer Excess")).selection("tool").named(mw.get("OUTER CONTACT CUTTER"));

                model.geom(id).create(mw.next("par","Cut Inner Excess"), "Partition");
                model.geom(id).feature(mw.get("Cut Inner Excess")).label("Cut Inner Excess"); // added this
                model.geom(id).feature(mw.get("Cut Inner Excess")).set("contributeto", mw.get("FINAL CONTACT"));
                model.geom(id).feature(mw.get("Cut Inner Excess")).selection("input").named(mw.get("CONTACT PRE CUTS"));
                model.geom(id).feature(mw.get("Cut Inner Excess")).selection("tool").named(mw.get("INNER CONTACT CUTTER"));

                model.geom(id).create(mw.next("ballsel","sel inner excess"), "BallSelection");
                model.geom(id).feature(mw.get("sel inner excess")).label("sel inner excess");
                model.geom(id).feature(mw.get("sel inner excess")).set("posx", "(r_inner_contact/2)*cos(rotation_angle)");
                model.geom(id).feature(mw.get("sel inner excess")).set("posy", "(r_inner_contact/2)*sin(rotation_angle)");
                model.geom(id).feature(mw.get("sel inner excess")).set("posz", "z_center");
                model.geom(id).feature(mw.get("sel inner excess")).set("r", 1);
                model.geom(id).feature(mw.get("sel inner excess")).set("contributeto", mw.get("SEL INNER EXCESS CONTACT"));

                model.geom(id).create(mw.next("ballsel","sel outer excess"), "BallSelection");
                model.geom(id).feature(mw.get("sel outer excess")).label("sel outer excess");
                model.geom(id).feature(mw.get("sel outer excess")).set("posx", "((r_outer_contact+2*r_cuff_in_Pitt)/2)*cos(rotation_angle)");
                model.geom(id).feature(mw.get("sel outer excess")).set("posy", "((r_outer_contact+2*r_cuff_in_Pitt)/2)*sin(rotation_angle)");
                model.geom(id).feature(mw.get("sel outer excess")).set("posz", "z_center");
                model.geom(id).feature(mw.get("sel outer excess")).set("r", 1);
                model.geom(id).feature(mw.get("sel outer excess")).set("contributeto", mw.get("SEL OUTER EXCESS CONTACT"));

                model.geom(id).create(mw.next("del","Delete Inner Excess Contact"), "Delete");
                model.geom(id).feature(mw.get("Delete Inner Excess Contact")).label("Delete Inner Excess Contact");
                model.geom(id).feature(mw.get("Delete Inner Excess Contact")).selection("input").init(3);
                model.geom(id).feature(mw.get("Delete Inner Excess Contact")).selection("input").named(mw.get("SEL INNER EXCESS CONTACT"));

                model.geom(id).create(mw.next("del","Delete Outer Excess Contact"), "Delete");
                model.geom(id).feature(mw.get("Delete Outer Excess Contact")).label("Delete Outer Excess Contact");
                model.geom(id).feature(mw.get("Delete Outer Excess Contact")).selection("input").init(3);
                model.geom(id).feature(mw.get("Delete Outer Excess Contact")).selection("input").named(mw.get("SEL OUTER EXCESS CONTACT"));

                model.geom(id).create(mw.next("if","If Recess"), "If");
                model.geom(id).feature(mw.get("If Recess")).set("condition", "recess_Pitt>0");
                model.geom(id).feature(mw.get("If Recess")).label("If Recess");

                model.geom(id).create(mw.next("wp","Recess Cross Section"), "WorkPlane");
                model.geom(id).feature(mw.get("Recess Cross Section")).label("Recess Cross Section");
                model.geom(id).feature(mw.get("Recess Cross Section")).set("contributeto", mw.get("RECESS CROSS SECTION"));
                model.geom(id).feature(mw.get("Recess Cross Section")).set("planetype", "transformed");
                model.geom(id).feature(mw.get("Recess Cross Section")).set("workplane", mw.get("base plane (pre rotation)"));
                model.geom(id).feature(mw.get("Recess Cross Section")).set("transaxis", new int[]{0, 1, 0});
                model.geom(id).feature(mw.get("Recess Cross Section")).set("transrot", "rotation_angle");
                model.geom(id).feature(mw.get("Recess Cross Section")).set("unite", true);
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().selection().create("csel1", "CumulativeSelection"); // TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().selection("csel1").label("CONTACT PRE FILLET");// TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().selection().create("csel2", "CumulativeSelection");// TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().selection("csel2").label("CONTACT FILLETED");// TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().selection().create("csel3", "CumulativeSelection");// TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().selection("csel3").label("RECESS PRE FILLET");// TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().selection().create("csel4", "CumulativeSelection");// TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().selection("csel4").label("RECESS FILLETED");// TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().create("r1", "Rectangle");
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("r1").label("Recess Pre Fillet Corners");// TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("r1").set("contributeto", "csel3");// TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("r1").set("pos", new int[]{0, 0});
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("r1").set("base", "center");
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("r1").set("size", new String[]{"w_contact_Pitt", "z_contact_Pitt"});
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().create("fil1", "Fillet"); // TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("fil1").label("Fillet Corners"); // TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("fil1").set("contributeto", "csel4");// TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("fil1").set("radius", "fillet_contact_Pitt");
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("fil1").selection("point").named("csel3");// TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().create("sca1", "Scale"); // TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("sca1").set("type", "anisotropic"); // TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("sca1") // TODO
                        .set("factor", new String[]{"1", "scale_morph_w_contact_Pitt"});
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("sca1").selection("input").named("csel4");// TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().create("mov1", "Move"); // TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("mov1").set("disply", "z_center"); // TODO
                model.geom(id).feature(mw.get("Recess Cross Section")).geom().feature("mov1").selection("input").named("csel4");// TODO

                model.geom(id).create(mw.next("ext", "Make Recess Pre Cuts 1"), "Extrude");
                model.geom(id).feature(mw.get("Make Recess Pre Cuts 1")).label("Make Recess Pre Cuts 1");
                model.geom(id).feature(mw.get("Make Recess Pre Cuts 1")).set("contributeto", mw.get("RECESS PRE CUTS"));
                model.geom(id).feature(mw.get("Make Recess Pre Cuts 1")).setIndex("distance", "2*r_cuff_in_Pitt", 0);
                model.geom(id).feature(mw.get("Make Recess Pre Cuts 1")).selection("input").named(mw.get("RECESS CROSS SECTION"));

                model.geom(id).create(mw.next("cyl", "Inner Recess Cutter"), "Cylinder");
                model.geom(id).feature(mw.get("Inner Recess Cutter")).label("Inner Recess Cutter");
                model.geom(id).feature(mw.get("Inner Recess Cutter")).set("contributeto", mw.get("INNER RECESS CUTTER"));
                model.geom(id).feature(mw.get("Inner Recess Cutter")).set("pos", new String[]{"0", "0", "-L_cuff_Pitt/2+z_center"});
                model.geom(id).feature(mw.get("Inner Recess Cutter")).set("r", "r_cuff_in_Pitt");
                model.geom(id).feature(mw.get("Inner Recess Cutter")).set("h", "L_cuff_Pitt");

                model.geom(id).create(mw.next("cyl","Outer Recess Cutter"), "Cylinder");
                model.geom(id).feature(mw.get("Outer Recess Cutter")).label("Outer Recess Cutter");
                model.geom(id).feature(mw.get("Outer Recess Cutter")).set("contributeto", mw.get("OUTER RECESS CUTTER"));
                model.geom(id).feature(mw.get("Outer Recess Cutter")).set("pos", new String[]{"0", "0", "-L_cuff_Pitt/2+z_center"});
                model.geom(id).feature(mw.get("Outer Recess Cutter")).set("r", "r_inner_contact");
                model.geom(id).feature(mw.get("Outer Recess Cutter")).set("h", "L_cuff_Pitt");

                model.geom(id).create(mw.next("par","Remove Outer Recess Excess"), "Partition");
                model.geom(id).feature(mw.get("Remove Outer Recess Excess")).label("Remove Outer Recess Excess");
                model.geom(id).feature(mw.get("Remove Outer Recess Excess")).set("contributeto", mw.get("FINAL RECESS"));
                model.geom(id).feature(mw.get("Remove Outer Recess Excess")).selection("input").named(mw.get("RECESS PRE CUTS"));
                model.geom(id).feature(mw.get("Remove Outer Recess Excess")).selection("tool").named(mw.get("OUTER RECESS CUTTER"));

                model.geom(id).create(mw.next("par","Remove Inner Recess Excess"), "Partition");
                model.geom(id).feature(mw.get("Remove Inner Recess Excess")).label("Remove Inner Recess Excess");
                model.geom(id).feature(mw.get("Remove Inner Recess Excess")).set("contributeto", mw.get("FINAL RECESS"));
                model.geom(id).feature(mw.get("Remove Inner Recess Excess")).selection("input").named(mw.get("RECESS PRE CUTS"));
                model.geom(id).feature(mw.get("Remove Inner Recess Excess")).selection("tool").named(mw.get("INNER RECESS CUTTER"));

                model.geom(id).create(mw.next("ballsel","sel inner excess 1"), "BallSelection");
                model.geom(id).feature(mw.get("sel inner excess 1")).label("sel inner excess 1");
                model.geom(id).feature(mw.get("sel inner excess 1")).set("posx", "((r_inner_contact+recess_Pitt)/2)*cos(rotation_angle)");
                model.geom(id).feature(mw.get("sel inner excess 1")).set("posy", "((r_inner_contact+recess_Pitt)/2)*sin(rotation_angle)");
                model.geom(id).feature(mw.get("sel inner excess 1")).set("posz", "z_center");
                model.geom(id).feature(mw.get("sel inner excess 1")).set("r", 1);
                model.geom(id).feature(mw.get("sel inner excess 1")).set("contributeto", mw.get("SEL INNER EXCESS RECESS"));

                model.geom(id).create(mw.next("ballsel","sel outer excess 1"), "BallSelection");
                model.geom(id).feature(mw.get("sel outer excess 1")).label("sel outer excess 1");
                model.geom(id).feature(mw.get("sel outer excess 1")).set("posx", "((r_cuff_in_Pitt+2*r_cuff_in_Pitt)/2)*cos(rotation_angle)");
                model.geom(id).feature(mw.get("sel outer excess 1")).set("posy", "((r_cuff_in_Pitt+2*r_cuff_in_Pitt)/2)*sin(rotation_angle)");
                model.geom(id).feature(mw.get("sel outer excess 1")).set("posz", "z_center");
                model.geom(id).feature(mw.get("sel outer excess 1")).set("r", 1);
                model.geom(id).feature(mw.get("sel outer excess 1")).set("contributeto", mw.get("SEL OUTER EXCESS RECESS"));

                model.geom(id).create(mw.next("del","Delete Inner Excess Recess"), "Delete");
                model.geom(id).feature(mw.get("Delete Inner Excess Recess")).label("Delete Inner Excess Recess");
                model.geom(id).feature(mw.get("Delete Inner Excess Recess")).selection("input").init(3);
                model.geom(id).feature(mw.get("Delete Inner Excess Recess")).selection("input").named(mw.get("SEL INNER EXCESS RECESS"));

                model.geom(id).create(mw.next("del","Delete Outer Excess Recess"), "Delete");
                model.geom(id).feature(mw.get("Delete Outer Excess Recess")).label("Delete Outer Excess Recess");
                model.geom(id).feature(mw.get("Delete Outer Excess Recess")).selection("input").init(3);
                model.geom(id).feature(mw.get("Delete Outer Excess Recess")).selection("input").named(mw.get("SEL OUTER EXCESS RECESS"));

                model.geom(id).create(mw.next("endif"), "EndIf");

                model.geom(id).create(mw.next("pt","src"), "Point");
                model.geom(id).feature(mw.get("src")).label("src");
                model.geom(id).feature(mw.get("src")).set("contributeto", mw.get("SRC"));
                model.geom(id).feature(mw.get("src"))
                        .set("p", new String[]{"(r_cuff_in_Pitt+recess_Pitt+(thk_contact_Pitt/2))*cos(rotation_angle)", "(r_cuff_in_Pitt+recess_Pitt+(thk_contact_Pitt/2))*sin(rotation_angle)", "z_center"});
                model.geom(id).run();
                break;
            default:
                throw new  IllegalArgumentException("No implementation for part primitive name: " + pseudonym);
        }

        // if im was not edited for some reason, return null
        if (im.count() == 0) return null;
        return im;
    }

    /**
     *
     * @param id
     * @param pseudonym
     * @param mw
     * @return
     */
    public static boolean createPartInstance(String id, String pseudonym, ModelWrapper2 mw) throws IllegalArgumentException {
        return createPartInstance(id, pseudonym, mw, null);
    }

    /**
     *
     * @param id
     * @param pseudonym
     * @param mw
     * @param data
     * @return
     */
    public static boolean createPartInstance(String id, String pseudonym, ModelWrapper2 mw, HashMap<String, Object> data) throws IllegalArgumentException {

        Model model = mw.getModel();
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").mesh().create("mesh1");

        // EXAMPLE
        String nextCsel = mw.next("csel", "mySuperCoolCsel");

        // you can either refer to it with that variable, nextCsel
        model.geom(id).selection().create(nextCsel, "CumulativeSelection");

        // or retrieve it later (likely in another method where the first variable isn't easily accessible
        model.geom(id).selection(mw.get("mySuperCoolCsel")).label("INNER CUFF SURFACE");



        switch (pseudonym) {
            case "TubeCuff_Primitive":
                break;
            case "RibbonContact_Primitive":
                break;
            case "WireContact_Primitive":
                break;
            case "CircleContact_Primitive":
                break;
            case "HelicalCuffnContact_Primitive":
                break;
            case "RectangleContact_Primitive":
                break;
            default:
                throw new IllegalArgumentException("No implementation for part instance name: " + pseudonym);
        }

        return true;
    }
}
