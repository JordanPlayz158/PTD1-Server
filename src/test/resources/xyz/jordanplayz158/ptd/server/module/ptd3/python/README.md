# PTD3-SERVER

This project aims to make the flash game Pokémon Tower Defense 3 playable again, by recreating the server and responding to the client's requests the same way the original would have.
It requires a slightly modified SWF client in order to use this server over the default (now dead/offline) one.

# How to Play
I recommend installing a personal server on [Deta Space](https://deta.space/discovery/@etrotta/ptd3server).
After installing it, open the server's app for more instructions.
Alternatively,
- You can use someone else's server to test it out, but keep in mind that your account could easily be griefed when doing so, 
    and it is not trivial to transfer your account to another server.
- If you are not afraid to touch the code on your own, you can clone this repository and run it locally by:
  - Cloning the repository via `git clone https://github.com/etrotta/PTD3-SERVER`
  - Installing the dependencies `python-dotenv`, `deta`, and `flask` via `pip install python-dotenv`, `pip install deta`, and `pip install flask` respectively
  - Copy the `.env.example` file to `src/local.env` and edit it if you wish
  - Run `flask --app src.main run -p 1234`
  - When logging in, point the URL to `http://localhost:1234`. What you use for the password does not matters.


Whichever method you use to get a server, you'll need of a PTD3 SWF modified so that it can point to the right server, as well as a functional flash player.
If you want to modify the game client yourself, see the mod_instructions.txt for instructions

# Links
- Server on Deta Space: https://deta.space/discovery/@etrotta/ptd3server
- Modified Game SWF: https://drive.google.com/file/d/1PlQRLNhsJxvJ_zu1CYGygHlw3vtzVRS2/view?usp=sharing
- Flash Player (Newgrounds): https://www.newgrounds.com/flash/player
- Flash Player (Adobe, debugger): https://web.archive.org/web/20220401020702/https://www.adobe.com/support/flashplayer/debug_downloads.html

# Contributing
Create a GitHub Issue or contact me on Discord first.
To run the tests, you may need to install `pip install pytest` and `pip -e .` in the project root directory

# Acknowledgements
This project wouldn't have been possible with the original game by [Samuel Otero](https://samdangames.blogspot.com/) and the [JPEXS Free Flash Decompiler](https://github.com/jindrapetrik/jpexs-decompiler).

Icon made with Stable Diffusion 2.1 on https://playgroundai.com/.

Shout-out to the [PTD Archives discord server](https://discord.gg/vMgTRW46cs).
