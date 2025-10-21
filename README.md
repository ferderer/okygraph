```xml
<plugin>
    <groupId>dev.okygraph</groupId>
    <artifactId>okygraph-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- Generate support classes -->
        <generateRuntime>true</generateRuntime>
        <runtimePackage>dev.okygraph.runtime</runtimePackage>

        <!-- Or use external dependency if preferred -->
        <!-- <generateRuntime>false</generateRuntime> -->
        <!-- <runtimeDependency>com.example:custom-runtime:1.0</runtimeDependency> -->

        <!-- Escaping configuration -->
        <escaping>
            <mode>AUTO</mode>  <!-- AUTO, MANUAL, or NONE -->
            <generateEscapers>true</generateEscapers>
        </escaping>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```