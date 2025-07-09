#!/bin/bash

git filter-branch --env-filter '
if [ $GIT_COMMIT = "1f15a03" ]
then
    export GIT_AUTHOR_DATE="$(git show -s --format=%ai $GIT_COMMIT)"
    export GIT_COMMITTER_DATE="$(git show -s --format=%ci $GIT_COMMIT)"
    export GIT_COMMIT_MSG="Update bottom app bar tab names: Gallery to Photos."
fi
' --msg-filter '
if [ $GIT_COMMIT = "1f15a03" ]
then
    echo "Update bottom app bar tab names: Gallery to Photos."
else
    cat
fi
' HEAD~2..HEAD
