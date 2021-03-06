def kibana(String branch){
  common.conditionalStage(
    stage_name: "Prepare Kibana Selenium",
    stage: {
      kibana_prep(branch)
    }
  )
  common.conditionalStage(
    stage_name: "Kibana Tests",
    stage: {
      kibana_tests(branch)
    }
  )
}

def kibana_prep(String branch){
  dir("kibana-selenium") {
    git url: env.KIBANA_SELENIUM_REPO, branch: "${branch}"

    sh """#!/bin/bash
      # The phantomjs package on 16.04 is buggy, see:
      # https://github.com/ariya/phantomjs/issues/14900
      # https://bugs.launchpad.net/ubuntu/+source/phantomjs/+bug/1578444
      #apt-get install -y phantomjs
      apt-get install -y fontconfig
      wget https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-linux-x86_64.tar.bz2
      tar -xjf phantomjs-2.1.1-linux-x86_64.tar.bz2

      if [[ ! -d ".venv" ]]; then
          pip install virtualenv
          virtualenv .venv
      fi
      set +x; source .venv/bin/activate; set -x
      if [ -f ~/.pip/pip.conf ]; then
        mv ~/.pip/pip.conf ~/.pip/pip.conf.bak
        pip install -r requirements.txt
        mv ~/.pip/pip.conf.bak ~/.pip/pip.conf
      else
        pip install -r requirements.txt
      fi
    """
  }
}

def kibana_tests(String branch){
  try {
    dir("kibana-selenium") {
      git url: env.KIBANA_SELENIUM_REPO, branch: "${branch}"
      sh """#!/bin/bash
        set +x; source .venv/bin/activate; set -x
        export PYTHONPATH=\$(pwd)
        export PATH=\$PATH:./phantomjs-2.1.1-linux-x86_64/bin
        # Remove any existing screenshots from old runs
        rm -f *.png

        export PASSWORD=\$(grep -Ir kibana_password /etc/openstack_deploy/ | tail -1 | sed 's/.*\\: //')
        python conf-gen.py --secure --password \$PASSWORD \
          --vip-file /etc/openstack_deploy/openstack_user_config.yml
        nosetests -sv --with-xunit testrepo/kibana/kibana.py
      """
    } // dir
  } catch (e){
    print(e)
    throw(e)
  } finally {
    junit allowEmptyResults: true, testResults: 'kibana-selenium/nosetests.xml'
    archiveArtifacts(
      allowEmptyArchive: true,
      artifacts: "kibana-selenium/*.png"
    ) //archive
  } // finally
}

return this;
