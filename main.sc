SFViewPlus {

	var <>fenetre, <>dir,
	<>sf, <>playEvent,
	t;

	*new{
		arg fen=Window.new.alwaysOnTop_(true).front,
		dir=Platform.resourceDir;
		^super.newCopyArgs(fen, dir).init;
	}
	init{
		var path=Platform.userHomeDir+/+dir;
		var layout;
		var rootPath, filePath, gridRes, gridResNUMBERBOX, gridResSLIDER, positionSLIDER, boutonPlay, offsetBox, amp, boutonServeur, t, tSERVEUR;
		var isPlaying;
		var getWAV={
			arg path;
			PathName(path).files
			.select({arg i;i.extension=="wav"}).collect(_.fullPath)
		};

		playEvent=();

		t=Task ({
			inf.do({
				arg i;
				sf.soundfile !? {
					if (i != 0) {
						AppClock.sched(0,
							{
								sf.timeCursorPosition =
								sf.timeCursorPosition + (sf.soundfile.sampleRate * TempoClock.beatDur);
								offsetBox.value=sf.timeCursorPosition;
							};
						)
				}};
				1.wait
			})
		});

		tSERVEUR=Task({
			inf.do{
				if(Server.local.serverRunning)
				{boutonServeur.value_(0)}
				{boutonServeur.value_(1)}
			};
			1.wait;
		});
		boutonServeur=Button()
		.states_([
			["serveur pas en marche"],
			["serveur en marche", Color.green, Color.red]
		])
		.action_{
			arg self;
			switch(self.value,
				1, {Server.local.boot},
				0, {Server.local.quit}
			)
		};

		offsetBox=NumberBox().value_(0);

		amp=Knob().value_(0.5)
		.action_{ arg self;
			playEvent !? {
				playEvent.synth.set(\amp, self.value)
			}
		};

		rootPath=Button()
		.states_([
			[path]
		])
		.action_{
			FileDialog({
				arg p;
				// on change le path
				path=p[0];
				// on actualise les paths
				rootPath.states_([
					[path]
				]);
				filePath.items_(
					getWAV.(path)
				)
			}, {}, 2,0, false )
		};

		filePath=PopUpMenu()
		.allowsReselection_(true)
		.items_(getWAV.(path))
		.action_{ arg self;
			var path=self.item;
			path.postln;
			sf.soundfile_(
				SoundFile.openRead(path)
			);
			sf.soundfile.postln;
			sf.readWithTask
		};

		sf=SoundFileView()
		.timeCursorOn_(true)
		.gridOn_(true)
		.keyDownAction_{

			// fonction pour declencher le depart

			arg self, char, mod, unicode;
			var ev;
			switch(unicode,
				32,
				{
					self.soundfile !? {
						switch(playEvent[\isPlaying].asBoolean,
							false, {
								playEvent=self.soundfile.cue(
									(firstFrame:self.timeCursorPosition, amp:amp.value), true);
								t.play;
								boutonPlay.value_(1);
							},
							true, {
								playEvent.stop;
								t.stop;
								boutonPlay.value_(0);
							}
						)
					}
				}
			);
		}
		.mouseUpAction_{

			// modification quand on deplace le curseur

			arg self, xpos, ypos, modif;

			var frame=sf.timeCursorPosition;
			var file=sf.soundfile;
			var amp=amp.value;

			var firstFrame, pos;

			if (file.isNil.not and: modif==0) {
				var total=sf.soundfile.numFrames,

				nbFen=(file.duration / sf.gridResolution),
				firstFrame=(sf.numFrames-sf.viewFrames)*sf.scrollPos,
				pos =ControlSpec(firstFrame, firstFrame+sf.viewFrames);

				//TODO: il faudrait prendre en compte le offset
				frame=frame.quantize(total / nbFen, total / nbFen, 1);  // stick to grid

				// stick to slider
				sf.timeCursorPosition=frame;
				positionSLIDER.valueAction_(
					pos.unmap(sf.timeCursorPosition)
				);

				// info boxes
				offsetBox.value=frame;

				// music
				self.soundfile !? {
					switch(playEvent[\isPlaying],
						true, {
							//stop and play
							playEvent.stop;
							playEvent=self.soundfile.cue(
								(firstFrame:self.timeCursorPosition, amp:amp.value), true);
						}
					)
				}
			};
		}.minHeight_(100);


		gridResNUMBERBOX=NumberBox()
		.value_(0)
		.maxWidth_(20)
		;

		gridResSLIDER=Slider()
		.value_(0.5)
		.action_{
			arg self;
			var firstFrame=(sf.numFrames-sf.viewFrames)*sf.scrollPos;
			var pos =ControlSpec(firstFrame, firstFrame+sf.viewFrames);
			var resolution=ControlSpec(0.3, 10, 'exp').map(self.value);

			//on change
			// la resolution
			sf.soundfile !? {
				sf.gridResolution_(resolution);
				positionSLIDER.valueAction_(pos.unmap(sf.timeCursorPosition))
			};
			// on actualise la numberBox
			gridResNUMBERBOX.value=resolution;
		}
		.maxWidth_(20)
		;

		positionSLIDER=Slider()
		.value_(0)
		.orientation_(\horizontal)
		.action_{
			arg self;
			var baseFrame, resolution, offset;
			var firstFrame=(sf.numFrames-sf.viewFrames)*sf.scrollPos;
			var pos =ControlSpec(firstFrame, firstFrame+sf.viewFrames);

			if(playEvent[\isPlaying].asBoolean.not) {
			var frame=pos.map(self.value);

				// on actualise le time curseur
			sf.timeCursorPosition_(frame);
			offsetBox.value_(frame);

				// on déplace par rapport au time curseur
				// TODO remplacer 44100 par le vrai server.local.samplerate
			baseFrame=sf.timeCursorPosition/44100;
			resolution=sf.gridResolution;
			offset=baseFrame-resolution;

			sf.gridOffset_(offset);
			}
		}
		.fixedWidth_(sf.bounds.width);

		boutonPlay=Button()
		.states_([
			[""],
			["", Color.red]
		])
		;

		layout=(VLayout(
			HLayout(rootPath, filePath),
			VLayout(
				HLayout(
					sf,
					VLayout(gridResNUMBERBOX, gridResSLIDER)
				),
				HLayout(positionSLIDER, boutonPlay, boutonServeur, offsetBox, amp)
			)
		));
		fenetre.layout_(layout);

	}
}


