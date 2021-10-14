# package installation
conda create -n ascent python==3.7 anaconda
conda activate ascent
pip install -r requirements.txt

echo
echo "Installation complete."
echo
# create shortcut
while true; do
    read -p "Add ASCENT environment setup alias to '.bash_profile'? (recommended) [y/N] " yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) echo "Not added"; exit 0;;
    esac
done

echo "alias ascent_setup='source $CONDA_SETUP_SCRIPT; conda activate ascent; cd $PWD'" >> ~/.bash_profile
echo "Added. Remember to run 'ascent_setup' to use (requires shell restart)."
exit 0
